package com.usuallycoding.openadhd.domain

object CoinRules {
    fun baseReward(tier: Tier): Int = when (tier) {
        Tier.One -> 5
        Tier.Two -> 10
        Tier.Three -> 20
        Tier.Four -> 40
    }

    fun balance(entries: List<LedgerEntry>): Int = entries.sumOf { it.amount }
}
