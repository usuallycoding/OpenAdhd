# Tasks

## 1. Domain and roll rules

- [x] 1.1 Add `RedemptionSource` and `rolledTier` to `Redemption` (defaults `Purchase` and null, plus a derived `isLuckyUpgrade`), and `LedgerReason.MysteryRoll`; verify `./kotlin build -m shared` compiles
- [x] 1.2 Add `RollRules` (`UpgradeOneIn = 6`, rollable tiers 1 to 4, roll price, `choose` drawing the upgrade first and the pick second) with unit tests using a scripted `Random` for: usual outcome, upgrade, upgrade falling back when the next tier is empty, tier 4 never upgrading, an empty pool giving null, tier 5 rewards never chosen, and the pick index mapping; verify `./kotlin test -m shared` passes

## 2. Stores and contract tests

- [x] 2.1 Add `ShopRepository.mysteryRoll(tier, redemptionId)` returning a `RollResult` or null, and implement it in `InMemoryStore`/`StoreLogic` with an injected `Random`: refuse on short balance, empty tier or reused id; otherwise record the redemption (source `Roll`, rolled tier, roll price paid) and a ledger entry of minus the roll price with the `MysteryRoll` reason; verify the build compiles
- [x] 2.2 Let the contract tests build stores with a scripted `Random`, and add cases for: a roll charging the tier price in both the usual and the upgrade outcome, the redemption and ledger fields, a short balance refused, an empty tier refused, a repeated roll id rolling once, the upgrade fallback, tier 4 never upgrading, tier 5 rewards never given, and a purchase still recorded as a purchase; verify the in-memory store passes

## 3. Room version 4

- [x] 3.1 Add the `source` and `rolledTier` columns to the redemption entity, a transactional `mysteryRoll` DAO method that reads the balance and pools, calls `RollRules.choose` and inserts the redemption and ledger entry, and the `Random` constructor parameter on `RoomStore`; bump the database to version 4; verify the shared contract suite passes against `RoomStore` on jvm
- [x] 3.2 Add the 3 to 4 `Migration` (two `ALTER TABLE` statements) and tests that open a version 3 database built with raw SQL (with a purchase) and a version 1 database through all migrations, asserting earlier purchases read as purchases, the balance is unchanged and a roll works afterwards; verify they pass on jvm
- [x] 3.3 Extend the restart test with a mystery roll and verify it is restored after reopening, still marked as a roll of the same tier, with the reduced balance

## 4. UI state

- [x] 4.1 Extend `ShopViewModel` with the four roll options (price, affordability, coins needed, empty tier), `roll(tier, rollId)`, and the last-roll state with `dismissRoll()`, and verify with view-model tests including a refused roll, a double confirm with the same id, and an upgrade result

## 5. Screens

- [x] 5.1 Add the Mystery roll section to the Shop's Rewards tab (four buttons with price and odds text, disabled with a reason when unaffordable or empty), the confirmation dialog and the reveal dialog (with the lucky-upgrade line); verify with a UI test using a scripted `Random` that a roll spends the price, shows the reward and the upgrade message when it upgrades, and that cancelling spends nothing
- [x] 5.2 Mark rolls in the History (note, rolled tier, upgrade flag), add previews, and verify with a UI test that a roll appears in the History as a mystery roll while earlier purchases stay plain purchases

## 6. Integration

- [x] 6.1 Run `./kotlin build` and `./kotlin test` for all targets and verify both succeed
- [x] 6.2 Verify on desktop with your existing database (back it up first): open the app, confirm templates, rewards, purchases and the balance are intact after the 3 to 4 migration, then make a mystery roll, check the balance and the History entry, restart, and confirm the roll persists
