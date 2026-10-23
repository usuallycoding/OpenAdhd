# Tasks

## 1. Domain model and price rules

- [x] 1.1 Add `RewardTier` (levels 1 to 5), `RewardTemplate` (with `description` and nullable `customPrice`), `Redemption`, and `LedgerReason.RewardRedeemed`; verify `./kotlin build -m shared` compiles
- [x] 1.2 Add `RewardRules` (price table 20/50/120/300, `MinCustomPrice` = 301, `price(tier, customPrice)`) and `RewardTemplate` validation (blank name, tier 5 price missing or at most 300, custom price cleared outside tier 5), with unit tests for every tier, the 300/301 boundary and the missing price; verify `./kotlin test -m shared` passes

## 2. Stores and contract tests

- [x] 2.1 Add `RewardRepository` and `ShopRepository` to `OpenAdhdStore` and implement them in `InMemoryStore`/`StoreLogic`: reward create/update/delete/list, atomic `redeem(rewardId, redemptionId)` (balance guard, redemption snapshot, negative ledger entry, same id is a no-op), and the redemptions flow; verify the build compiles
- [x] 2.2 Extend the store contract tests to cover: reward create/edit/delete with validation and tier-5 price rules, leaving tier 5 clears the custom price, redeem success (balance down by the price, ledger -price with `RewardRedeemed` and the redemption id), redeem refused when short (nothing recorded), exact balance spends to 0, same redemption id twice records one redemption, two separate redemptions of one reward, redemption keeps name and price after the reward is edited or deleted, and history order newest first; verify the in-memory store passes

## 3. Room version 3

- [x] 3.1 Add the reward and redemption entities, DAO queries and a transactional `redeem` (balance read, redemption insert and ledger insert in one transaction), implement them in `RoomStore`, and bump the database to version 3; verify the shared contract suite passes against `RoomStore` on jvm
- [x] 3.2 Add the 2 to 3 `Migration` (two `CREATE TABLE` statements only) and tests that open a version 2 database built with raw SQL and a version 1 database through both migrations, asserting templates, entries and the balance are unchanged and the reward and redemption tables are empty and usable; verify they pass on jvm
- [x] 3.3 Extend the restart test with a reward, a redemption and the reduced balance, and verify they are all restored after reopening

## 4. UI state

- [x] 4.1 Add `RewardTemplatesViewModel` (list, save with validation errors for name and tier 5 price, delete) and `ShopViewModel` (rewards grouped by tier with tier 5 sorted by price, affordability with coins still needed, redeem, history), and verify with view-model tests including a refused overdraw and the grouping and ordering rules

## 5. Screens

- [x] 5.1 Add the Tasks | Rewards tab switch to the Templates screen, the Rewards list (tier, energy, price, empty state) and the reward dialog (five tier chips, price field only for tier 5, inline errors); verify with a UI test that creates a tier 2 reward (price 50), rejects a tier 5 price of 300, and accepts 301
- [x] 5.2 Add the Shop destination to the bottom navigation with a Rewards | History view: rewards grouped by tier with price and "N more coins" when short, a confirmation dialog, and the history list with empty states; verify with a UI test that earning coins, redeeming a reward and confirming lowers the balance and adds the history entry, that cancelling spends nothing, and that an unaffordable reward cannot be redeemed
- [x] 5.3 Add icons and previews, and update the README data note to mention rewards and purchases; verify the note matches what is stored

## 6. Integration

- [x] 6.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 6.2 Verify on desktop with your existing database (back it up first): open the app, confirm templates, entries and the balance are intact after the 2 to 3 migration, then create a reward, earn enough coins, redeem it, restart, and confirm the purchase and balance persist
