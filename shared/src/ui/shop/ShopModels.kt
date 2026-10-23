package com.usuallycoding.openadhd.ui.shop

import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier

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
                ShopItem(reward, affordable = balance >= reward.price, coinsNeeded = maxOf(0, reward.price - balance))
            },
        )
    }
