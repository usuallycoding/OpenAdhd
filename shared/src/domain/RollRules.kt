package com.usuallycoding.openadhd.domain

import kotlin.random.Random

data class RollOutcome(val reward: RewardTemplate, val upgraded: Boolean)

/** What a roll returns: the recorded redemption, and whether it was a lucky upgrade. */
data class RollResult(val redemption: Redemption, val upgraded: Boolean)

object RollRules {
    /** Tier 5 prices are custom, so it can neither be rolled nor won. */
    val RollableTiers = listOf(RewardTier.One, RewardTier.Two, RewardTier.Three, RewardTier.Four)

    /** A roll costs what the tier costs. */
    fun price(tier: RewardTier, rules: GameRules = GameRules.Default): Int =
        requireNotNull(RewardRules.fixedPrice(tier, rules)) { "Tier ${tier.level} cannot be rolled" }

    /** The tier a roll of [tier] can upgrade to; tier 4 and above have none. */
    fun nextTier(tier: RewardTier): RewardTier? = when (tier) {
        RewardTier.One -> RewardTier.Two
        RewardTier.Two -> RewardTier.Three
        RewardTier.Three -> RewardTier.Four
        RewardTier.Four, RewardTier.Five -> null
    }

    /**
     * Picks the reward for a roll of [rolledTier]. The upgrade decision (one in [GameRules.upgradeOneIn]) is always drawn first, then the pick
     * within the chosen tier, so a scripted [random] gives predictable results. Returns null when the rolled
     * tier has no rewards. An upgrade only happens when the next tier has rewards.
     */
    fun choose(
        random: Random,
        rolledTier: RewardTier,
        rewards: List<RewardTemplate>,
        rules: GameRules = GameRules.Default,
    ): RollOutcome? {
        require(rolledTier in RollableTiers) { "Tier ${rolledTier.level} cannot be rolled" }
        val pool = rewards.filter { it.tier == rolledTier }
        if (pool.isEmpty()) return null
        val wantsUpgrade = random.nextInt(rules.upgradeOneIn) == 0
        val upgradePool = nextTier(rolledTier)?.let { next -> rewards.filter { it.tier == next } }.orEmpty()
        return if (wantsUpgrade && upgradePool.isNotEmpty()) {
            RollOutcome(upgradePool[random.nextInt(upgradePool.size)], upgraded = true)
        } else {
            RollOutcome(pool[random.nextInt(pool.size)], upgraded = false)
        }
    }
}
