package com.usuallycoding.openadhd.domain

import kotlin.random.Random

/** Picks a task template for the user's current energy; see the task-randomizer spec. */
object TaskPicker {
    private fun Energy.rank(): Int = when (this) {
        Energy.Low -> 0
        Energy.Medium -> 1
        Energy.High -> 2
    }

    /**
     * How likely a template is relative to the others: 4 when its energy equals the [chosen] energy, 2 when it is
     * one level below, 1 when it is two levels below, and 0 (never) when it needs more energy than [chosen].
     */
    fun weight(chosen: Energy, templateEnergy: Energy): Int = when (chosen.rank() - templateEnergy.rank()) {
        0 -> 4
        1 -> 2
        2 -> 1
        else -> 0
    }

    /** The templates that can be suggested, in their given order. */
    fun eligible(chosen: Energy, templates: List<TaskTemplate>, excludedTemplateIds: Set<String>): List<TaskTemplate> =
        templates.filter { it.id !in excludedTemplateIds && weight(chosen, it.energy) > 0 }

    /**
     * Picks one eligible template with a chance proportional to its weight, using a single draw over the
     * templates in their given order. Returns null when nothing is eligible, without drawing.
     */
    fun pick(
        random: Random,
        chosen: Energy,
        templates: List<TaskTemplate>,
        excludedTemplateIds: Set<String> = emptySet(),
    ): TaskTemplate? {
        val candidates = eligible(chosen, templates, excludedTemplateIds)
        if (candidates.isEmpty()) return null
        var remaining = random.nextInt(candidates.sumOf { weight(chosen, it.energy) })
        for (candidate in candidates) {
            remaining -= weight(chosen, candidate.energy)
            if (remaining < 0) return candidate
        }
        return candidates.last() // unreachable: the draw is below the total weight
    }
}
