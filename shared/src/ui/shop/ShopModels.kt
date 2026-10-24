package com.usuallycoding.openadhd.ui.shop

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.RollRules

/** A reward as the shop shows it, given the current balance. */
data class ShopItem(val reward: RewardTemplate, val affordable: Boolean, val coinsNeeded: Int)

/** All the rewards of one tier. */
data class ShopShelf(val tier: RewardTier, val items: List<ShopItem>)

/**
 * Groups [rewards] by tier from 1 to 5, leaving out empty tiers. Tier 5 rewards, which each have their own
 * price, are ordered from cheapest to dearest; the other tiers keep their given order.
 */
fun buildShelves(rewards: List<RewardTemplate>, balance: Int): List<ShopShelf> =
    RewardTier.entries.mapNotNull { tier ->
        val inTier = rewards.filter { it.tier == tier }
        if (inTier.isEmpty()) return@mapNotNull null
        val ordered = if (tier == RewardTier.Five) inTier.sortedBy { it.price } else inTier
        ShopShelf(
            tier,
            ordered.map { reward ->
                ShopItem(reward, affordable = CoinRules.canPay(balance, reward.price), coinsNeeded = CoinRules.coinsNeeded(balance, reward.price))
            },
        )
    }

/** One mystery roll as the shop shows it. [upgradeTier] is set only when the next tier has rewards to upgrade to. */
data class RollOption(
    val tier: RewardTier,
    val price: Int,
    val affordable: Boolean,
    val coinsNeeded: Int,
    val hasRewards: Boolean,
    val upgradeTier: RewardTier?,
) {
    val canRoll: Boolean get() = affordable && hasRewards
}

/** The four rolls, tiers 1 to 4, given the rewards that exist and the current balance. */
fun buildRollOptions(rewards: List<RewardTemplate>, balance: Int): List<RollOption> =
    RollRules.RollableTiers.map { tier ->
        val price = RollRules.price(tier)
        RollOption(
            tier = tier,
            price = price,
            affordable = CoinRules.canPay(balance, price),
            coinsNeeded = CoinRules.coinsNeeded(balance, price),
            hasRewards = rewards.any { it.tier == tier },
            upgradeTier = RollRules.nextTier(tier)?.takeIf { next -> rewards.any { it.tier == next } },
        )
    }
