# Proposal

## Why

Coins can be earned but not spent: the ledger only ever goes up, so nothing yet gives the points a purpose. The reward shop closes the loop (task, coins, reward) that the app is built around, and it is the foundation the planned mystery rolls and Dopamine Loans depend on.

## What Changes

- Add reward templates: reusable rewards with a name, optional description, an energy level (how much energy the reward itself takes) and a tier from 1 to 5.
- Add fixed prices for tiers 1 to 4 (20, 50, 120 and 300 coins). Tier 5 has a custom price entered per reward, which must be larger than the tier 4 price (301 or more).
- Manage reward templates on the existing Templates screen, in a new Rewards tab beside Tasks.
- Add a Shop screen: rewards grouped by tier with their price, how many coins short the user is when a reward is not affordable, and a confirmation step before redeeming.
- Redeeming spends the price: a negative ledger entry plus a redemption record that freezes the reward's name and price at purchase. The balance can never go below zero.
- Add a purchase history inside the Shop.
- Move the local database to a new schema version that only adds tables; existing data is untouched.

## Capabilities

### New Capabilities
- `reward-templates`: Creating, editing, deleting and listing reward templates, and how a reward's price follows from its tier.
- `reward-shop`: Browsing rewards in the shop, affordability, redeeming, and the redemption history.

### Modified Capabilities
- `coin-economy`: the ledger also records spending, the balance includes it, and spending that would overdraw the balance is refused.
- `local-data-storage`: reward templates and redemptions are persisted, and data survives the upgrade from the previous version.

## Impact

- New domain, store, Room (version 3 with a migration that only creates tables), view-model and UI code in `shared`; a third bottom navigation destination (Shop); a Tasks/Rewards tab switch on the Templates screen; new icons.
- No new libraries.
- Assumptions to confirm later: tier 5 exists for rewards only (task tiers stay 1 to 4); the minimum custom price is 301; there are no per-day or per-reward purchase limits; a purchase cannot be undone or refunded; the shop shows energy but does not yet filter by it.
- Out of scope, planned as later changes: mystery rolls, Dopamine Loans (negative balance), a "today's reward" goal on the Today screen, Critical Hit and Wall of Awful bonuses, the smart randomizer, and streak protection.
