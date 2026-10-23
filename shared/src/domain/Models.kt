package com.usuallycoding.openadhd.domain

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

enum class Tier(val level: Int) {
    One(1), Two(2), Three(3), Four(4);

    companion object {
        fun fromLevel(level: Int): Tier = entries.first { it.level == level }
    }
}

enum class Energy { Low, Medium, High }

/** A reusable task. Subtasks are plain titles: a flat, ordered list that cannot nest. */
data class TaskTemplate(
    val id: String,
    val name: String,
    val tier: Tier,
    val energy: Energy,
    val description: String = "",
    val subtasks: List<String> = emptyList(),
)

/** One step of a [PlannedTask], worth [reward] coins when completed. */
data class PlannedSubtask(
    val id: String,
    val title: String,
    val reward: Int,
    val completedAt: Instant? = null,
) {
    val isCompleted: Boolean get() = completedAt != null
}

/**
 * A task assigned to a day. Everything is copied from the template at assignment time, including
 * [reward] (the tier's coin pool) and each subtask's share, so later template edits change nothing here.
 */
data class PlannedTask(
    val id: String,
    val date: LocalDate,
    val name: String,
    val tier: Tier,
    val energy: Energy,
    val templateId: String?,
    /** When a task without subtasks was completed; unused for tasks that have subtasks. */
    val completedAt: Instant? = null,
    val description: String = "",
    val reward: Int = CoinRules.baseReward(tier),
    val subtasks: List<PlannedSubtask> = emptyList(),
) {
    val isCompleted: Boolean
        get() = if (subtasks.isEmpty()) completedAt != null else subtasks.all { it.isCompleted }

    /** True once any part of the task has paid out coins. */
    val hasProgress: Boolean
        get() = if (subtasks.isEmpty()) completedAt != null else subtasks.any { it.isCompleted }
}

enum class LedgerReason { TaskCompleted, SubtaskCompleted }

data class LedgerEntry(
    val id: String,
    val amount: Int,
    val timestamp: Instant,
    val reason: LedgerReason,
    val refId: String? = null,
)
