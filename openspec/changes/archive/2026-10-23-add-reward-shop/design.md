# Design

## Context

Earning is complete (see `openspec/specs/`): task `Tier` is 1 to 4 with fixed pools, the ledger is append-only (`LedgerEntry(amount, timestamp, reason, refId)`), and the balance is the sum of the ledger. Persistence is Room KMP, database version 2, behind the `OpenAdhdStore` interface with an in-memory implementation and one shared contract test suite. The app has two bottom destinations (Today, Templates), one `TemplatesScreen` for task templates, and per-feature view models. Nothing has spent coins yet. See proposal.md for scope.

## Goals / Non-Goals

**Goals:**
- Reward templates with a five-tier price ladder, a shop that sells them, and an atomic, balance-safe redeem.
- A purchase history that stays correct when rewards are edited or deleted.
- A migration that only adds tables.

**Non-Goals:**
- Mystery rolls, Dopamine Loans (negative balance), a "today's reward" goal, purchase limits, refunds or undo, filtering the shop by energy, and any change to task tiers or earning.

## Decisions

- **Reward tier is its own type.** `RewardTier` has levels 1 to 5 and is separate from the task `Tier` (1 to 4, with pools and a subtask limit tied to it). Tier 5 therefore exists only for rewards and cannot leak into task logic.
- **Prices live in one pure object.** `RewardRules.price(tier, customPrice)` returns 20/50/120/300 for tiers 1 to 4 and the custom price for tier 5. `RewardRules.MinCustomPrice` is the tier 4 price plus one (301). A reward stores `tier` and a nullable `customPrice`; `customPrice` is required for tier 5 and must be null otherwise (editing a reward out of tier 5 clears it). Validation (`validated()`, as for task templates) trims text, rejects a blank name, a missing or too-low tier 5 price, and normalises the price away for other tiers; both stores call it, so the rules are enforced where data is written.
- **Redemption is a snapshot.** `Redemption(id, name, tier, energy, price, redeemedAt, templateId?)` copies everything at purchase time and has no foreign key to the reward, so editing or deleting a reward never changes history (the same approach as `PlannedTask`).
- **Spending reuses the ledger.** `LedgerReason` gains `RewardRedeemed`; a redemption appends `LedgerEntry(amount = -price, reason = RewardRedeemed, refId = redemption id)`. The balance stays the sum of the ledger, so nothing else changes.
- **Redeem is one atomic operation with a guard.** `redeem(rewardId, redemptionId)` checks the balance against the price, inserts the redemption and appends the ledger entry in a single transaction, and returns false (changing nothing) if the reward is missing, the balance is too low, or that `redemptionId` already exists. In Room it is a `@Transaction` DAO method that reads `COALESCE(SUM(amount), 0)` from the ledger inside the transaction (SQLite has a single writer, so two concurrent redeems cannot both pass the check); in memory it is a compare-and-set on the whole state. The "balance never negative" rule is the same function, so loans can later relax it in one place.
- **Double activation is a no-op.** The confirm dialog generates a fresh `redemptionId` when it opens and passes it with the call. The primary key makes a second call with the same id return false, which implements "confirming twice quickly records one redemption" without relying on UI timing alone; the dialog also closes on the first confirm.
- **Store API.** New repository interfaces `RewardRepository` (create/update/delete/list reward templates) and `ShopRepository` (redemptions flow, `redeem`) are added to the combined `OpenAdhdStore`, with the same validate-on-write behavior and the same contract tests run against the in-memory and Room stores.
- **Room version 3, additive migration.** New tables `reward_templates(id, name, tier INTEGER, energy TEXT, description TEXT, customPrice INTEGER nullable)` and `redemptions(id, templateId TEXT nullable, name, tier INTEGER, energy TEXT, price INTEGER, redeemedAtMillis INTEGER)`. The 2 to 3 `Migration` only runs two `CREATE TABLE` statements and touches no existing table, which is why it is low risk. It is tested on a version 2 database built with raw SQL and also on a version 1 database through both migrations (1 to 2 to 3).
- **UI structure.** The Templates screen gets a `TabRow` (Tasks | Rewards); the Rewards tab uses a `RewardTemplatesViewModel` and a reward dialog (name, description, five tier chips, energy chips, and a numeric price field shown only for tier 5, with inline errors for a missing or too-low price). The Shop is a third bottom destination with its own `ShopViewModel` (rewards grouped by tier with affordability, redeem, history) and two tabs inside it (Rewards | History). Affordability is derived from the balance the wallet already exposes. Redeeming always goes through a confirmation dialog showing the reward and its price.
- **No new libraries.** Icons are code-defined vectors like the others.

## Risks / Trade-offs

- [A mistaken purchase cannot be undone] → confirmation dialog before spending; a refund would be a compensating positive ledger entry in a later change.
- [Concurrent redeems overdraw the balance] → the check and the writes share one transaction, covered by a contract test that redeems beyond the balance.
- [A user later raises the tier 4 price so an old tier 5 reward ends up at or below it] → nothing breaks because the price is stored with the reward; the minimum applies only on save, and the design note is enough for now.
- [Long shop lists] → a lazy list grouped by tier; no limit on the number of rewards.
- [Migration regressions] → additive tables only, plus tests from versions 1 and 2.
- [Energy is stored and shown but unused] → intentional; the future randomizer and shop filter will use it.

## Migration Plan

1. Ship database version 3 with the additive migration; no manual step and no data export.
2. A failing migration must fail loudly in tests, never fall back to destroying data.
3. Downgrading is not supported, which is acceptable for an unreleased app.

## Open Questions

- Whether a purchase should be refundable (a compensating ledger entry) can wait for after the first use; it would add a requirement and one ledger reason, not change the data model.
