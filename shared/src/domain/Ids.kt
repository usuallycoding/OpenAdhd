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
internal fun TaskTemplate.validated(rules: GameRules = GameRules.Default): TaskTemplate {
    val titles = subtasks.map { title ->
        title.trim().also { require(it.isNotEmpty()) { "Every subtask needs a title" } }
    }
    val limit = CoinRules.maxSubtasks(tier, rules)
    require(titles.size <= limit) { "A tier ${tier.level} task can have at most $limit subtasks" }
    val problem = ScheduleRules.check(schedule, reminder)
    require(problem == null) { problem!!.message }
    return copy(name = requireValidName(name), description = description.trim(), subtasks = titles)
}

/** Builds the entry for [date] from a template: copies its fields and freezes every coin amount. */
internal fun TaskTemplate.toPlannedTask(date: kotlinx.datetime.LocalDate, rules: GameRules = GameRules.Default): PlannedTask {
    val pool = CoinRules.baseReward(tier, rules)
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

/** The entry a schedule adds on [date]: the template as [toPlannedTask] copies it, plus the template's reminder on that day. */
internal fun TaskTemplate.generatedTask(
    date: kotlinx.datetime.LocalDate,
    zone: kotlinx.datetime.TimeZone,
    rules: GameRules = GameRules.Default,
): PlannedTask = toPlannedTask(date, rules).copy(
    reminder = reminder?.let { Reminder(ReminderRules.at(date, it.hour, it.minute, zone), it.repeatMinutes) },
)

/** Trims text fields, clears the custom price outside tier 5, and rejects invalid input. */
internal fun RewardTemplate.validated(rules: GameRules = GameRules.Default): RewardTemplate {
    val problem = RewardRules.validate(name, tier, customPrice, rules)
    require(problem == null) { "Invalid reward: $problem" }
    return copy(
        name = name.trim(),
        description = description.trim(),
        customPrice = if (tier == RewardTier.Five) customPrice else null,
    ).let { it.priced(rules) }
}
