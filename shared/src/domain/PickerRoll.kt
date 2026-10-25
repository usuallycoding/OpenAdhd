package com.usuallycoding.openadhd.domain

import kotlin.random.Random

/** The dice in the Add to today picker: one of the templates shown, all equally likely; see the task-randomizer spec. */
object PickerRoll {
    /**
     * Draws one of [shown], or null when it is empty without drawing. When more than one is shown the template with
     * id [previousId] is left out, so a roll does not repeat the last one; with one shown it is returned as it is.
     */
    fun pick(random: Random, shown: List<TaskTemplate>, previousId: String?): TaskTemplate? {
        if (shown.isEmpty()) return null
        if (shown.size == 1) return shown.single()
        val candidates = shown.filter { it.id != previousId }
        return candidates[random.nextInt(candidates.size)]
    }
}
