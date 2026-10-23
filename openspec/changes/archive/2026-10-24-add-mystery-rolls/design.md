# Design

## Context

The shop is in place (see `openspec/specs/`): `RewardTemplate` with `RewardTier` 1 to 5 and fixed prices for tiers 1 to 4, an atomic `redeem(rewardId, redemptionId)` that checks the balance, writes a `Redemption` snapshot and a negative ledger entry in one transaction, and a Shop screen with Rewards and History tabs. Persistence is Room KMP at database version 3 behind `OpenAdhdStore`, with an in-memory implementation and one contract test suite run against both. Randomness is not used anywhere yet. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- A roll that is atomic, balance-safe, idempotent per confirmation, and fully testable with scripted randomness.
- Rolls that are visible in the History and the ledger without breaking any existing purchase.

**Non-Goals:**
- Animation, rerolls, pity timers, Critical Hit on rolls, tier 5 in rolls, any change to shop pricing, and statistics about past rolls.

## Decisions

- **Roll rules are a pure function.** A `RollRules` object holds the constants (`UpgradeOneIn = 6`, rollable tiers 1 to 4, the price of a roll = `RewardRules.fixedPrice(tier)`) and `choose(random, rolledTier, rewards): RollOutcome?`. It draws the upgrade decision first (`random.nextInt(6) == 0`), then the index within the chosen tier's pool (`random.nextInt(pool.size)`), always in that order so scripted tests are predictable. The upgrade only applies when the next tier's pool is not empty (otherwise it falls back to the rolled tier and the outcome is not an upgrade); tier 4 has no next rollable tier; tier 5 rewards are never in any pool; an empty rolled pool gives null (roll refused).
- **`Redemption` gains how it was obtained.** `RedemptionSource { Purchase, Roll }` and `rolledTier: RewardTier?` (set only for rolls) are added with defaults (`Purchase`, `null`), so existing constructors keep compiling. A lucky upgrade is derived (`rolledTier != null && tier > rolledTier`), never stored, so it cannot disagree with the data.
- **A roll is a redemption plus a ledger entry.** The ledger gets `LedgerReason.MysteryRoll`; the entry is minus the roll price with `refId` = the redemption id, exactly like a purchase. The price paid stored on the redemption is the roll price, not the won reward's own price.
- **Store API.** `ShopRepository.mysteryRoll(tier, redemptionId = newId()): RollResult?` returns the redemption and whether it was an upgrade, or null (changing nothing) when the balance is too low, the rolled tier has no rewards, or the `redemptionId` was already used. The same repeated-id idea as `redeem` makes a double confirm roll once.
- **Randomness is injected.** Both stores take a `Random` (default `Random.Default`) in their constructor. The in-memory store runs the roll inside its compare-and-set loop (a retry draws again, which is harmless). In Room the DAO runs one `@Transaction`: it reads the balance and the rolled and next tiers' rewards, calls `RollRules.choose`, then inserts the redemption and the ledger entry. SQLite's single writer keeps the balance check and the writes together. The contract tests construct stores with a scripted `Random` to force each branch.
- **Room version 4, columns only.** The 3 to 4 migration runs `ALTER TABLE redemptions ADD COLUMN source TEXT NOT NULL DEFAULT 'Purchase'` and `ADD COLUMN rolledTier INTEGER` (nullable). It touches an existing table but only adds columns with safe defaults, so every earlier purchase reads back as a purchase. It is tested on a version 3 database built with raw SQL (including a purchase) and on a version 1 database through all migrations.
- **Shop view model.** `ShopViewModel` exposes the four roll options (tier, price, affordable, coins still needed, whether the tier has rewards) derived from the rewards and the balance, a `roll(tier, rollId)` action, and the last roll result as state that the reveal dialog shows and `dismissRoll()` clears.
- **Shop UI.** A "Mystery roll" section at the top of the Rewards tab with four buttons, each showing its tier and price and explaining the odds ("1 in 6 chance of a tier 3 reward"); a button is disabled when unaffordable or the tier is empty and says why. Tapping opens a confirmation (tier, price, odds) that creates a fresh roll id when it opens, then a reveal dialog with the reward and, for upgrades, a "Lucky upgrade! Tier 2 roll, tier 3 reward" line. The History shows rolls with a "Mystery roll" note, the rolled tier and the upgrade flag.
- **No new libraries.**

## Risks / Trade-offs

- [Rolling beats buying on average, which may make rolls feel mandatory] → the edge is modest (about 25% for tiers 1 to 3) and the odds are one constant; buying stays the way to get a specific reward.
- [Uniform picks can repeat the same reward] → accepted; a no-repeat rule is a possible follow-up.
- [A tier's pool is small, so outcomes are predictable] → inherent to a personal reward list; the upgrade adds variety when the next tier has rewards.
- [Randomness inside a database transaction is hard to test] → the random source is injected and the choice itself is a pure function, tested directly and through the contract suite.
- [The migration alters an existing table] → columns only, with defaults, plus tests from versions 3 and 1.
- [A mistaken roll cannot be undone] → confirmation first; refunds are not supported, as with purchases.

## Migration Plan

1. Ship database version 4 with the additive migration; no manual step.
2. A failing migration must fail loudly in tests rather than recreate the database.
3. Downgrading is not supported, which is acceptable for an unreleased app.

## Open Questions

- Whether upgrades should ever reach into tier 5, and whether to avoid repeating the last reward, can wait for real use; each would change `RollRules` and one requirement, not the data model.
