package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CriticalHits

/** A [CriticalHits] that answers from a fixed list, in order, to force a particular sequence of completions. */
class ScriptedCrits(vararg answers: Boolean) : CriticalHits {
    private val queue = ArrayDeque(answers.toList())

    val remaining: Int get() = queue.size

    /** The chance each draw was asked with, in order. */
    val asked = mutableListOf<Int>()

    override fun isCritical(oneIn: Int): Boolean {
        asked += oneIn
        return queue.removeFirst()
    }
}
