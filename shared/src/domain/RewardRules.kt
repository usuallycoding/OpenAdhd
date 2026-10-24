package com.usuallycoding.openadhd.domain

enum class RewardInputError { NameRequired, PriceRequired, PriceTooLow }

object RewardRules {
    /** A tier 5 reward must cost more than the most expensive fixed tier. */
    fun minCustomPrice(rules: GameRules = GameRules.Default): Int = rules.rewardPrices.last() + 1

    /** The fixed price of tiers 1 to 4, or null for tier 5 whose price is entered per reward. */
    fun fixedPrice(tier: RewardTier, rules: GameRules = GameRules.Default): Int? = rules.price(tier)

    /** @throws IllegalArgumentException for a tier 5 reward without a custom price. */
    fun price(tier: RewardTier, customPrice: Int?, rules: GameRules = GameRules.Default): Int =
        fixedPrice(tier, rules) ?: requireNotNull(customPrice) { "A tier 5 reward needs a custom price" }

    /** What is wrong with this input, or null when it can be saved. */
    fun validate(name: String, tier: RewardTier, customPrice: Int?, rules: GameRules = GameRules.Default): RewardInputError? = when {
        name.isBlank() -> RewardInputError.NameRequired
        tier == RewardTier.Five && customPrice == null -> RewardInputError.PriceRequired
        tier == RewardTier.Five && customPrice!! < minCustomPrice(rules) -> RewardInputError.PriceTooLow
        else -> null
    }
}
