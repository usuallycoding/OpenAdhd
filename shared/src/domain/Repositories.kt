package com.usuallycoding.openadhd.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface TemplateRepository {
    val templates: Flow<List<TaskTemplate>>

    /**
     * Description and subtask titles are trimmed.
     * @throws IllegalArgumentException if [name] or a subtask title is blank, or there are more subtasks than the tier's coin pool or the subtask ceiling allow ([CoinRules.maxSubtasks]).
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

    /** Puts the templates in the order of [ids]; see [reorderedBy] for how unknown and missing ids are treated. */
    suspend fun reorderTemplates(ids: List<String>)
}

interface PlannerRepository {
    fun entriesFor(date: LocalDate): Flow<List<PlannedTask>>

    /** Every day that has at least one entry, today included. */
    val daysWithEntries: Flow<Set<LocalDate>>

    /** Copies the template onto [date], freezing the coin reward of the task and of each subtask. */
    suspend fun assign(template: TaskTemplate, date: LocalDate): PlannedTask

    /** Removes an entry; entries with any completed part are left alone so earned coins are never orphaned. */
    suspend fun remove(id: String)

    /**
     * Sets, replaces or (with null) clears the reminder of an entry. Returns false, changing nothing, if the entry
     * is unknown or already completed. Completing an entry later keeps its reminder, so unchecking brings it back;
     * the reminder rules decide what is still to fire.
     */
    suspend fun setReminder(entryId: String, reminder: Reminder?): Boolean

    /** Puts the entries of [date] in the order of [ids]; other days are untouched. See [reorderedBy]. */
    suspend fun reorderEntries(date: LocalDate, ids: List<String>)

    /**
     * Completes a task that has no subtasks and appends its ledger entry as one operation. With a chance set
     * by the settings and drawn by the store's [CriticalHits], it is a critical hit and also appends a bonus entry.
     * Returns null, changing nothing and not drawing, if the entry is unknown, has subtasks, or is already completed.
     */
    suspend fun completeTask(id: String): CompletionResult?

    /**
     * Completes one subtask and appends its ledger entry as one operation, with the same critical hit chance.
     * Returns null, changing nothing and not drawing, if the subtask is unknown or already completed.
     */
    suspend fun completeSubtask(id: String): CompletionResult?

    /**
     * Unchecks a completed task that has no subtasks and appends one ledger entry taking back everything it has
     * paid (reward and critical-hit bonus, less earlier undos), as one operation. Returns the coins taken back,
     * or null, changing nothing, if the entry is unknown, has subtasks, or is not completed.
     */
    suspend fun uncompleteTask(id: String): Int?

    /** Unchecks a completed subtask with the same one-entry reversal. Returns null, changing nothing, if it is unknown or not completed. */
    suspend fun uncompleteSubtask(id: String): Int?
}

interface LedgerRepository {
    val entries: Flow<List<LedgerEntry>>
}

interface RewardRepository {
    val rewards: Flow<List<RewardTemplate>>

    /**
     * Name and description are trimmed; a custom price is kept for tier 5 only.
     * @throws IllegalArgumentException if [name] is blank, or tier 5 has no custom price above the tier 4 price ([RewardRules.minCustomPrice]).
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

    /** Puts the reward templates in the order of [ids]; see [reorderedBy]. */
    suspend fun reorderRewards(ids: List<String>)
}

interface ShopRepository {
    /** Purchases, newest first. */
    val redemptions: Flow<List<Redemption>>

    /**
     * Buys a reward: records a redemption that freezes its name and price, and appends a ledger entry of
     * minus the price, as one operation. Returns false, changing nothing, if the reward is unknown, the
     * payment would pass the debt limit ([GameRules.debtLimit]), or a redemption with [redemptionId] already exists (so a repeated
     * call with the same id is a no-op).
     */
    suspend fun redeem(rewardId: String, redemptionId: String = newId()): Boolean

    /**
     * Rolls for a random reward of [tier] (1 to 4): costs the tier's price and gives a reward from that tier,
     * or with a 1 in [GameRules.upgradeOneIn] chance from the next tier up. Records a redemption marked as a
     * roll and a ledger entry of minus the price as one operation. Returns null, changing nothing, if the
     * payment would pass the debt limit, the tier has no rewards, or [redemptionId] already exists.
     */
    suspend fun mysteryRoll(tier: RewardTier, redemptionId: String = newId()): RollResult?
}

interface SettingsRepository {
    /** The settings in force: the defaults with the user's changes on top. */
    val settings: Flow<Settings>

    /**
     * Saves [text] as the value of [key] after [checkSetting] accepts it, as one operation with the read of
     * the data it is checked against. A value equal to the default stores nothing, so it counts as restored.
     * Returns why it was refused, or null when it was saved.
     */
    suspend fun saveSetting(key: SettingKey, text: String): String?

    /** Puts the default back for [key] and forgets the saved value. */
    suspend fun restoreSetting(key: SettingKey)
}

interface DataRepository {
    /** Everything the app holds, in the order the app shows it. */
    suspend fun exportData(): StoreState

    /**
     * Replaces everything with [data] as one operation: nothing of the old data is kept or merged, and a failure
     * leaves the old data as it was. The data is assumed to have been validated (see the backup format).
     */
    suspend fun importData(data: StoreState)

    /** Removes everything, settings included, so the store is as when new. */
    suspend fun deleteAllData()
}

/** All repositories behind one object, so completing a task or buying a reward updates its records and the ledger together. */
interface OpenAdhdStore : TemplateRepository, PlannerRepository, LedgerRepository, RewardRepository, ShopRepository, SettingsRepository, DataRepository

/**
 * This list reordered to follow [ids]: items named in [ids] come first in that order, unknown ids are ignored,
 * and items not named keep their relative order after them, so a stale call can never lose an item.
 */
fun <T> List<T>.reorderedBy(ids: List<String>, idOf: (T) -> String): List<T> {
    val byId = associateBy(idOf)
    val listed = ids.distinct().mapNotNull { byId[it] }
    val listedIds = listed.map(idOf).toSet()
    return listed + filterNot { idOf(it) in listedIds }
}

/** These ids with [id] moved [offset] places (-1 up, +1 down); unchanged when [id] is absent or the move would leave the list. */
fun List<String>.moved(id: String, offset: Int): List<String> {
    val from = indexOf(id)
    val to = from + offset
    if (from < 0 || to !in indices) return this
    return toMutableList().also { it.add(to, it.removeAt(from)) }
}
