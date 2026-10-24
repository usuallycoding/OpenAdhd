package com.usuallycoding.openadhd.domain

import kotlin.random.Random

object CriticalRules {
    /** The extra coins a critical hit pays on top of [reward]; whole coins, since the multiplier is whole. */
    fun bonus(reward: Int, rules: GameRules = GameRules.Default): Int = reward * (rules.criticalMultiplier - 1)
}

/** Decides whether a completion is a critical hit. Called once per successful completion. */
fun interface CriticalHits {
    /** Whether this completion is a critical hit, with a chance of 1 in [oneIn] (from the settings). */
    fun isCritical(oneIn: Int): Boolean

    companion object {
        /** No critical hits ever: the default for stores, so tests stay deterministic. */
        val Never = CriticalHits { _ -> false }

        /** The real game: a draw of 1 in the chance from the settings. */
        fun standard(random: Random = Random.Default): CriticalHits {
            return CriticalHits { oneIn -> random.nextInt(oneIn) == 0 }
        }

        val Standard: CriticalHits = standard()
    }
}

/** What completing a task or subtask paid: the normal [reward] and the critical hit [bonus], if any. */
data class CompletionResult(val reward: Int, val bonus: Int = 0) {
    val isCritical: Boolean get() = bonus > 0
}
