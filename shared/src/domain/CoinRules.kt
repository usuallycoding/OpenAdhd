package com.usuallycoding.openadhd.domain

object CoinRules {
    /** The coin pool of a task of this [tier]. */
    fun baseReward(tier: Tier): Int = when (tier) {
        Tier.One -> 5
        Tier.Two -> 10
        Tier.Three -> 20
        Tier.Four -> 40
    }

    /** No task has more subtasks than this, whatever its tier, to keep lists short. */
    const val SubtaskCeiling = 10

    /** The most subtasks a task of this [tier] can have: never more than its pool, so each is worth 1 coin or more. */
    fun maxSubtasks(tier: Tier): Int = minOf(baseReward(tier), SubtaskCeiling)

    /**
     * Splits [pool] across [parts] subtasks, in order: each gets the pool divided by [parts], rounded down,
     * with the leftover coins going one each to the first parts. Because [parts] cannot exceed [pool], every
     * share is at least 1 and the shares add up exactly to the pool.
     */
    fun splitPool(pool: Int, parts: Int): List<Int> {
        require(parts in 1..pool) { "A pool of $pool coins cannot be split into $parts parts" }
        val base = pool / parts
        val extra = pool % parts
        return List(parts) { index -> if (index < extra) base + 1 else base }
    }

    fun balance(entries: List<LedgerEntry>): Int = entries.sumOf { it.amount }
}
