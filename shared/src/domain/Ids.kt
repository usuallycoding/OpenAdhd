package com.usuallycoding.openadhd.domain

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newId(): String = Uuid.random().toString()

internal fun requireValidName(name: String): String {
    val trimmed = name.trim()
    require(trimmed.isNotEmpty()) { "Name is required" }
    return trimmed
}

/** Trims text fields and rejects a blank name, blank subtask titles and more subtasks than the tier allows. */
internal fun TaskTemplate.validated(): TaskTemplate {
    val titles = subtasks.map { title ->
        title.trim().also { require(it.isNotEmpty()) { "Every subtask needs a title" } }
    }
    val limit = CoinRules.maxSubtasks(tier)
    require(titles.size <= limit) { "A tier ${tier.level} task can have at most $limit subtasks" }
    return copy(name = requireValidName(name), description = description.trim(), subtasks = titles)
}

/** Builds the entry for [date] from a template: copies its fields and freezes every coin amount. */
internal fun TaskTemplate.toPlannedTask(date: kotlinx.datetime.LocalDate): PlannedTask {
    val pool = CoinRules.baseReward(tier)
    val shares = if (subtasks.isEmpty()) emptyList() else CoinRules.splitPool(pool, subtasks.size)
    return PlannedTask(
        id = newId(),
        date = date,
        name = name,
        tier = tier,
        energy = energy,
        templateId = id,
        description = description,
        reward = pool,
        subtasks = subtasks.mapIndexed { index, title -> PlannedSubtask(newId(), title, shares[index]) },
    )
}

/** Trims text fields, clears the custom price outside tier 5, and rejects invalid input. */
internal fun RewardTemplate.validated(): RewardTemplate {
    val problem = RewardRules.validate(name, tier, customPrice)
    require(problem == null) { "Invalid reward: $problem" }
    return copy(
        name = name.trim(),
        description = description.trim(),
        customPrice = if (tier == RewardTier.Five) customPrice else null,
    )
}
