package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CriticalHits

/** A [CriticalHits] that answers from a fixed list, in order, to force a particular sequence of completions. */
class ScriptedCrits(vararg answers: Boolean) : CriticalHits {
    private val queue = ArrayDeque(answers.toList())

    val remaining: Int get() = queue.size

    override fun isCritical(): Boolean = queue.removeFirst()
}
