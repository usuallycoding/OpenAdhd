package com.usuallycoding.openadhd.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TemplateRepository {
    val templates: Flow<List<TaskTemplate>>

    /**
     * Description and subtask titles are trimmed.
     * @throws IllegalArgumentException if [name] or a subtask title is blank, or there are more subtasks than the tier's coin pool allows ([CoinRules.maxSubtasks]).
     */
    suspend fun create(
        name: String,
        tier: Tier,
        energy: Energy,
        description: String = "",
        subtasks: List<String> = emptyList(),
    ): TaskTemplate

    /** @throws IllegalArgumentException for the same invalid input as [create]. */
    suspend fun update(template: TaskTemplate)

    suspend fun delete(id: String)
}

interface PlannerRepository {
    fun entriesFor(date: LocalDate): Flow<List<PlannedTask>>

    /** Copies the template onto [date], freezing the coin reward of the task and of each subtask. */
    suspend fun assign(template: TaskTemplate, date: LocalDate): PlannedTask

    /** Removes an entry; entries with any completed part are left alone so earned coins are never orphaned. */
    suspend fun remove(id: String)

    /**
     * Completes a task that has no subtasks and appends its ledger entry as one operation.
     * Returns false, changing nothing, if the entry is unknown, has subtasks, or is already completed.
     */
    suspend fun completeTask(id: String): Boolean

    /**
     * Completes one subtask and appends its ledger entry as one operation.
     * Returns false, changing nothing, if the subtask is unknown or already completed.
     */
    suspend fun completeSubtask(id: String): Boolean
}

interface LedgerRepository {
    val entries: Flow<List<LedgerEntry>>
}

interface RewardRepository {
    val rewards: Flow<List<RewardTemplate>>

    /**
     * Name and description are trimmed; a custom price is kept for tier 5 only.
     * @throws IllegalArgumentException if [name] is blank, or tier 5 has no custom price above [RewardRules.TopFixedPrice].
     */
    suspend fun createReward(
        name: String,
        tier: RewardTier,
        energy: Energy,
        description: String = "",
        customPrice: Int? = null,
    ): RewardTemplate

    /** @throws IllegalArgumentException for the same invalid input as [createReward]. */
    suspend fun updateReward(reward: RewardTemplate)

    /** Deleting a reward keeps its redemptions and ledger entries. */
    suspend fun deleteReward(id: String)
}

interface ShopRepository {
    /** Purchases, newest first. */
    val redemptions: Flow<List<Redemption>>

    /**
     * Buys a reward: records a redemption that freezes its name and price, and appends a ledger entry of
     * minus the price, as one operation. Returns false, changing nothing, if the reward is unknown, the
     * balance is lower than the price, or a redemption with [redemptionId] already exists (so a repeated
     * call with the same id is a no-op).
     */
    suspend fun redeem(rewardId: String, redemptionId: String = newId()): Boolean
}

/** All repositories behind one object, so completing a task or buying a reward updates its records and the ledger together. */
interface OpenAdhdStore : TemplateRepository, PlannerRepository, LedgerRepository, RewardRepository, ShopRepository
