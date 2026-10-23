# Proposal

## Why

The shop only sells rewards you pick yourself, which is predictable. Variable, surprising rewards are a core part of the app's idea: a mystery roll turns spending coins into something exciting while staying anti-guilt, because a roll can never give you less than the tier you paid for. It also removes the decision of which reward to buy.

## What Changes

- Add mystery rolls for tiers 1 to 4. A roll costs the tier's fixed price (20, 50, 120 or 300 coins) and gives a random reward from that tier.
- With a 1 in 6 chance, the roll instead gives a random reward from the next tier up (a "lucky upgrade"). Tier 4 never upgrades, and tier 5 rewards are never rolled. If the next tier has no rewards there is no upgrade.
- A roll is refused, changing nothing, if the balance is too low or the rolled tier has no rewards.
- Rolling goes through a confirmation, happens once per confirmation, and shows a reveal of the reward won (with a "Lucky upgrade!" line when it happened).
- The won reward is recorded as a redemption: it keeps the reward's name and the price paid, and also records that it came from a roll and which tier was rolled. The History marks rolls and upgrades.
- The ledger gets a `MysteryRoll` reason so the balance stays explainable.
- Move the local database to a new schema version that adds two columns to the redemptions table; existing purchases become plain purchases.

## Capabilities

### New Capabilities
- `mystery-rolls`: Rolling for a random reward in a tier, the upgrade chance, what a roll costs, its confirmation and reveal, and when it is refused.

### Modified Capabilities
- `reward-shop`: a redemption also records how it was obtained, and the History marks mystery rolls and lucky upgrades.
- `coin-economy`: the ledger records a mystery roll as a spend with its own reason.
- `local-data-storage`: how a redemption was obtained is persisted, and purchases made before this version survive the upgrade.

## Impact

- Domain (`RollRules`, an extended `Redemption`, a new ledger reason), both stores, Room (version 4 with a migration that adds columns to an existing table), the Shop view model and the Shop screen (a Mystery roll section, a confirmation and a reveal dialog, History labels) in `shared`, with tests for each.
- No new libraries. Randomness is injected into the stores so tests can force every branch.
- Assumptions to confirm later: the pick within a tier is uniform; rolls need a confirmation like purchases do; there is no animation, no reroll and no "pity" mechanic; the 1 in 6 chance is a single constant that can be tuned.
- Out of scope, planned as later changes: Critical Hit and Wall of Awful bonuses, Dopamine Loans, a "today's reward" goal, the smart randomizer and streak protection.
