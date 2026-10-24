# Design

## Context

The Shop's History tab is `HistoryList(history: List<Redemption>)`, fed by `ShopViewModel.history` from `store.redemptions` (newest first). A `Redemption` stores the name, tier, energy, price, time, source and rolled tier, and the id of its reward template (`templateId`, which can be null or point at a reward that was since changed or deleted); it has no description. Redemptions are written in two places, `redeemReward` and `mysteryRoll` in the DAO, mirrored by `InMemoryStore`. The Today screen's History (`day-history`) has its date bar and calendar dialog inside `HistoryScreen`, its day rule in `HistoryDays`, and takes the current date from `TodayViewModel.today`. The database is at version 5 with migrations 1 to 5 in `OpenAdhdDatabase.kt`. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- One date bar and calendar dialog used by both History tabs, so they cannot drift apart.
- The Today screen's History behaves exactly as before.
- A redemption is a record: its description is fixed when it is made.

**Non-Goals:**
- Totals per day, filtering, search, and saving the chosen day; any change to `day-history`'s requirements.

## Decisions

- **Snapshot the description on the redemption.** `Redemption` and `RedemptionEntity` get `description: String = ""` (column default `''`). `redeemReward` and `mysteryRoll` copy it from the reward they already load, and `InMemoryStore` does the same. This follows the rule the module already uses for task entries and for the rest of a redemption (name, tier, energy, price): later edits and deletes must not rewrite history. Looking the description up through `templateId` was rejected because it would change with edits and be blank after a delete.
- **Migration 5 to 6.** `ALTER TABLE redemptions ADD COLUMN description TEXT NOT NULL DEFAULT ''`, then `UPDATE redemptions SET description = COALESCE((SELECT description FROM reward_templates WHERE reward_templates.id = redemptions.templateId), '')`. This is the best available guess for old rows: it uses the reward's current text if it still exists, which may differ from what it said at purchase, and blank otherwise. Nothing else is touched, so the ledger and balance do not change. `Migration5To6` is added to the builder and the version becomes 6.
- **Days come from the redemptions already in memory.** The new view model combines `store.redemptions` with the current date and a time zone: the days with redemptions are `redeemedAt.toLocalDateTime(timeZone).date` for each, and the day list filters the same list. The list is small and already held in memory, so a DAO query for distinct days would add a second source of truth for no gain. The time zone is a constructor parameter (default the device's), so tests can pin it.
- **A dedicated `RewardHistoryViewModel`.** It takes the store, the shared `today` flow (as `HistoryViewModel` does) and a time zone, and exposes `date`, `canGoNext`, `daysWithRedemptions`, `redemptions` (the chosen day, newest first) and `previousDay`, `nextDay`, `pickDay`. The chosen date is a nullable explicit choice; when null the date is today, so it follows the clock past midnight, and an explicit choice is clamped to at most today. It is created in `App` next to `HistoryViewModel`, so its state lives as long as the app, like the other tab state. `ShopViewModel.history` is no longer needed for the screen and is removed if nothing else uses it.
- **One day rule with an `includeToday` switch.** `HistoryDays.isSelectable` and `isSelectableYear` get an `includeToday: Boolean = false` parameter, and `SelectableHistoryDays` passes it on. Today's History keeps the default (before today only), so `day-history` is unchanged; the reward history passes `true` (today or earlier).
- **Extract the date bar and the calendar dialog.** The date button, the previous and next arrows (next hidden when `canGoNext` is false, space kept) and `DayPickerDialog` move from `HistoryScreen` into a shared composable that takes the date, `canGoNext`, the day set, `today`, `includeToday` and the three callbacks. `HistoryScreen` uses it with `includeToday = false`; the existing History UI tests stay as they are and must keep passing, which is the check that nothing changed there. The date label formatting moves with it.
- **Reward history list.** Replaces `HistoryList` with the shared date bar over a `LazyColumn` of the chosen day's redemptions. Each row keeps the current name, note and `-price`, and adds the description as a line under the name when it is not blank. The empty message is "Nothing redeemed on this day". The existing note text (with its date and time) is kept as it is; showing only the time on a single-day view is a refinement left out.
- **Spec home.** The day navigation and the day rule are added to `reward-shop` rather than a new capability, because they belong to the shop's redemption history; `day-history` is not touched.

## Risks / Trade-offs

- [The backfill may give an old redemption a description that was edited since] → accepted and stated in the spec: it uses the reward's text if it still exists, and nothing otherwise.
- [Local-date grouping depends on the device's time zone, so a redemption near midnight can move days if the zone changes] → acceptable for a personal device; the time zone is injectable and covered by a test with a zone far from UTC.
- [Moving the date bar could change Today's History by accident] → the existing `HistoryUiTest` and `HistoryDaysTest` run unchanged, and `includeToday` defaults to the old rule.
- [Removing the all-time list means no single place to scroll through every purchase] → agreed with the user; the calendar shows which days have redemptions.
- [Room schema version 6 must be reachable from every earlier version] → the migration test starts from version 5 and a chain test from an earlier version if the existing migration tests do the same.

## Migration Plan

Room runs `Migration5To6` on first start after the update; the new column has a default, so a failed backfill update cannot lose rows. Rolling back the app would need a database downgrade, which is not supported, so the change ships forward only (the same as earlier versions).
