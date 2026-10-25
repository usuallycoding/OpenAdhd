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
    /** When the template is added to Today by itself. */
    val schedule: TaskSchedule = TaskSchedule.None,
    /** The reminder given to the entries the schedule generates. */
    val reminder: TemplateReminder? = null,
    /** The last day an entry was generated for; stops a deleted entry coming back. Kept by the store, not set by the user. */
    val lastGenerated: LocalDate? = null,
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
    /** A reminder that fires until the task is done; kept while the task is completed, so unchecking brings it back. */
    val reminder: Reminder? = null,
) {
    val isCompleted: Boolean
        get() = if (subtasks.isEmpty()) completedAt != null else subtasks.all { it.isCompleted }

    /**
     * When the task was done: its own completion time, or for a task with subtasks the time its most recently
     * completed subtask was completed. Null while the task is not completed.
     */
    val doneAt: Instant?
        get() = if (subtasks.isEmpty()) completedAt else if (isCompleted) subtasks.maxOf { it.completedAt!! } else null

    /** True once any part of the task has paid out coins. */
    val hasProgress: Boolean
        get() = if (subtasks.isEmpty()) completedAt != null else subtasks.any { it.isCompleted }
}

enum class LedgerReason { TaskCompleted, SubtaskCompleted, RewardRedeemed, MysteryRoll, CriticalHit, Undone }

data class LedgerEntry(
    val id: String,
    val amount: Int,
    val timestamp: Instant,
    val reason: LedgerReason,
    val refId: String? = null,
)

/** Reward tiers 1 to 5. Separate from the task [Tier] (1 to 4): only tier 5 has a custom price. */
enum class RewardTier(val level: Int) {
    One(1), Two(2), Three(3), Four(4), Five(5);

    companion object {
        fun fromLevel(level: Int): RewardTier = entries.first { it.level == level }
    }
}

/**
 * A reusable reward. [customPrice] is set for tier 5 only; other tiers are priced by the settings. [price] is
 * what it costs under the settings in force when the store produced it, so it follows a changed tier price.
 */
data class RewardTemplate(
    val id: String,
    val name: String,
    val tier: RewardTier,
    val energy: Energy,
    val description: String = "",
    val customPrice: Int? = null,
    val price: Int = RewardRules.fixedPrice(tier) ?: customPrice ?: 0,
) {
    /** This reward with its [price] worked out under [rules]. */
    fun priced(rules: GameRules): RewardTemplate = copy(price = RewardRules.fixedPrice(tier, rules) ?: customPrice ?: 0)
}

enum class RedemptionSource { Purchase, Roll }

/**
 * A purchase or a mystery roll. Everything is copied from the reward when it is obtained, so later edits change
 * nothing here. [price] is what was paid (the roll price for a roll, not the won reward's own price), and
 * [rolledTier] is the tier that was rolled, set only for rolls.
 */
data class Redemption(
    val id: String,
    val templateId: String?,
    val name: String,
    val tier: RewardTier,
    val energy: Energy,
    val price: Int,
    val redeemedAt: Instant,
    val source: RedemptionSource = RedemptionSource.Purchase,
    val rolledTier: RewardTier? = null,
    /** The reward's description when it was redeemed; later edits to the reward do not change it. */
    val description: String = "",
) {
    /** A roll that gave a reward from a higher tier than the one rolled. */
    val isLuckyUpgrade: Boolean get() = rolledTier != null && tier.level > rolledTier.level
}

/** Pending entries first in the order the user put them, then completed ones, the most recently done first. */
fun List<PlannedTask>.pendingThenDone(): List<PlannedTask> {
    val (done, pending) = partition { it.isCompleted }
    return pending + done.sortedByDescending { it.doneAt }
}

/**
 * Ids of entries with subtasks whose subtasks are hidden. A pending entry is expanded and a completed one
 * collapsed unless its id is in [flipped], which turns that default around.
 */
fun List<PlannedTask>.collapsedIds(flipped: Set<String>): Set<String> =
    filter { it.subtasks.isNotEmpty() && (it.id in flipped) == !it.isCompleted }.map { it.id }.toSet()
