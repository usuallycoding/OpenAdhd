package com.usuallycoding.openadhd

import kotlin.random.Random

/** A [Random] that returns the given values from `nextInt(until)`, in order, to force a particular roll. */
class ScriptedRandom(vararg values: Int) : Random() {
    private val queue = ArrayDeque(values.toList())

    val remaining: Int get() = queue.size

    /** The exclusive upper bound of each draw, in order. */
    val bounds = mutableListOf<Int>()

    override fun nextBits(bitCount: Int): Int = error("Only nextInt(until) is scripted")

    override fun nextInt(until: Int): Int {
        bounds += until
        val value = queue.removeFirst()
        require(value in 0 until until) { "Scripted value $value is not below $until" }
        return value
    }
}
