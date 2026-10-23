package com.usuallycoding.openadhd.domain

enum class RewardInputError { NameRequired, PriceRequired, PriceTooLow }

object RewardRules {
    /** The price of tier 4, the most expensive fixed tier. */
    const val TopFixedPrice = 300

    /** A tier 5 reward must cost more than the most expensive fixed tier. */
    const val MinCustomPrice = TopFixedPrice + 1

    /** The fixed price of tiers 1 to 4, or null for tier 5 whose price is entered per reward. */
    fun fixedPrice(tier: RewardTier): Int? = when (tier) {
        RewardTier.One -> 20
        RewardTier.Two -> 50
        RewardTier.Three -> 120
        RewardTier.Four -> TopFixedPrice
        RewardTier.Five -> null
    }

    /** @throws IllegalArgumentException for a tier 5 reward without a custom price. */
    fun price(tier: RewardTier, customPrice: Int?): Int =
        fixedPrice(tier) ?: requireNotNull(customPrice) { "A tier 5 reward needs a custom price" }

    /** What is wrong with this input, or null when it can be saved. */
    fun validate(name: String, tier: RewardTier, customPrice: Int?): RewardInputError? = when {
        name.isBlank() -> RewardInputError.NameRequired
        tier == RewardTier.Five && customPrice == null -> RewardInputError.PriceRequired
        tier == RewardTier.Five && customPrice!! < MinCustomPrice -> RewardInputError.PriceTooLow
        else -> null
    }
}
