package com.usuallycoding.openadhd.domain

import kotlin.random.Random

object CriticalRules {
    /** One completion in this many is a critical hit. */
    const val OneIn = 8

    /** A critical hit pays this many times the normal reward in total (the bonus is the extra part). */
    const val Multiplier = 2

    /** The extra coins a critical hit pays on top of [reward]; whole coins, since the multiplier is whole. */
    fun bonus(reward: Int): Int = reward * (Multiplier - 1)
}

/** Decides whether a completion is a critical hit. Called once per successful completion. */
fun interface CriticalHits {
    fun isCritical(): Boolean

    companion object {
        /** No critical hits ever: the default for stores, so tests stay deterministic. */
        val Never = CriticalHits { false }

        fun oneIn(n: Int, random: Random = Random.Default): CriticalHits {
            require(n > 0) { "The chance must be one in a positive number" }
            return CriticalHits { random.nextInt(n) == 0 }
        }

        /** The real game: one completion in [CriticalRules.OneIn]. */
        val Standard: CriticalHits = oneIn(CriticalRules.OneIn)
    }
}

/** What completing a task or subtask paid: the normal [reward] and the critical hit [bonus], if any. */
data class CompletionResult(val reward: Int, val bonus: Int = 0) {
    val isCritical: Boolean get() = bonus > 0
}
