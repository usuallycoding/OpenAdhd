package com.usuallycoding.openadhd.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.usuallycoding.openadhd.domain.CompletionResult
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.CriticalRules
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.RollRules
import com.usuallycoding.openadhd.domain.reorderedBy
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

@Dao
abstract class OpenAdhdDao {
    // ---- templates

    @Transaction
    @Query("SELECT * FROM templates ORDER BY position, rowid")
    abstract fun templates(): Flow<List<TemplateWithSubtasks>>

    @Insert
    abstract suspend fun insertTemplate(template: TemplateEntity)

    @Update
    abstract suspend fun updateTemplate(template: TemplateEntity)

    @Insert
    abstract suspend fun insertTemplateSubtasks(subtasks: List<TemplateSubtaskEntity>)

    @Query("DELETE FROM template_subtasks WHERE templateId = :templateId")
    abstract suspend fun deleteTemplateSubtasks(templateId: String)

    @Query("DELETE FROM templates WHERE id = :id")
    abstract suspend fun deleteTemplateRow(id: String)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM templates")
    abstract suspend fun nextTemplatePosition(): Int

    @Query("SELECT position FROM templates WHERE id = :id")
    abstract suspend fun templatePosition(id: String): Int?

    @Query("SELECT id FROM templates ORDER BY position, rowid")
    abstract suspend fun templateIds(): List<String>

    @Query("UPDATE templates SET position = :position WHERE id = :id")
    abstract suspend fun setTemplatePosition(id: String, position: Int)

    @Transaction
    open suspend fun reorderTemplates(ids: List<String>) {
        templateIds().reorderedBy(ids) { it }.forEachIndexed { index, id -> setTemplatePosition(id, index) }
    }

    @Transaction
    open suspend fun insertTemplateWithSubtasks(template: TemplateEntity, subtasks: List<TemplateSubtaskEntity>) {
        insertTemplate(template.copy(position = nextTemplatePosition()))
        insertTemplateSubtasks(subtasks)
    }

    @Transaction
    open suspend fun updateTemplateWithSubtasks(template: TemplateEntity, subtasks: List<TemplateSubtaskEntity>) {
        // @Update writes every column, so carry the stored position over or an edit would move the template.
        updateTemplate(template.copy(position = templatePosition(template.id) ?: template.position))
        deleteTemplateSubtasks(template.id)
        insertTemplateSubtasks(subtasks)
    }

    @Transaction
    open suspend fun deleteTemplate(id: String) {
        deleteTemplateSubtasks(id)
        deleteTemplateRow(id)
    }

    // ---- planned tasks

    @Transaction
    @Query("SELECT * FROM planned_tasks WHERE date = :date ORDER BY position, rowid")
    abstract fun plannedFor(date: String): Flow<List<PlannedWithSubtasks>>

    @Insert
    abstract suspend fun insertPlanned(task: PlannedTaskEntity)

    @Insert
    abstract suspend fun insertPlannedSubtasks(subtasks: List<PlannedSubtaskEntity>)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM planned_tasks WHERE date = :date")
    abstract suspend fun nextPlannedPosition(date: String): Int

    @Query("SELECT id FROM planned_tasks WHERE date = :date ORDER BY position, rowid")
    abstract suspend fun plannedIds(date: String): List<String>

    @Query("UPDATE planned_tasks SET position = :position WHERE id = :id")
    abstract suspend fun setPlannedPosition(id: String, position: Int)

    @Transaction
    open suspend fun reorderPlanned(date: String, ids: List<String>) {
        plannedIds(date).reorderedBy(ids) { it }.forEachIndexed { index, id -> setPlannedPosition(id, index) }
    }

    @Transaction
    open suspend fun insertPlannedWithSubtasks(task: PlannedTaskEntity, subtasks: List<PlannedSubtaskEntity>) {
        insertPlanned(task.copy(position = nextPlannedPosition(task.date)))
        insertPlannedSubtasks(subtasks)
    }

    /** Deletes an entry only if nothing in it has been completed; returns how many rows were removed. */
    @Query(
        """DELETE FROM planned_tasks WHERE id = :id AND completedAtMillis IS NULL
           AND NOT EXISTS (SELECT 1 FROM planned_subtasks WHERE plannedTaskId = :id AND completedAtMillis IS NOT NULL)""",
    )
    abstract suspend fun deletePlannedIfUntouched(id: String): Int

    @Query("DELETE FROM planned_subtasks WHERE plannedTaskId = :plannedTaskId")
    abstract suspend fun deletePlannedSubtasks(plannedTaskId: String)

    @Transaction
    open suspend fun removePlanned(id: String) {
        if (deletePlannedIfUntouched(id) > 0) deletePlannedSubtasks(id)
    }

    // ---- ledger

    @Query("SELECT * FROM ledger ORDER BY rowid")
    abstract fun ledger(): Flow<List<LedgerEntity>>

    @Insert
    abstract suspend fun insertLedger(entry: LedgerEntity)

    // ---- rewards and redemptions

    @Query("SELECT * FROM reward_templates ORDER BY position, rowid")
    abstract fun rewards(): Flow<List<RewardTemplateEntity>>

    @Insert
    abstract suspend fun insertReward(reward: RewardTemplateEntity)

    @Update
    abstract suspend fun updateReward(reward: RewardTemplateEntity)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM reward_templates")
    abstract suspend fun nextRewardPosition(): Int

    @Query("SELECT position FROM reward_templates WHERE id = :id")
    abstract suspend fun rewardPosition(id: String): Int?

    @Query("SELECT id FROM reward_templates ORDER BY position, rowid")
    abstract suspend fun rewardIds(): List<String>

    @Query("UPDATE reward_templates SET position = :position WHERE id = :id")
    abstract suspend fun setRewardPosition(id: String, position: Int)

    @Transaction
    open suspend fun insertRewardLast(reward: RewardTemplateEntity) {
        insertReward(reward.copy(position = nextRewardPosition()))
    }

    /** Keeps the stored position, because @Update writes every column. */
    @Transaction
    open suspend fun updateRewardKeepingPosition(reward: RewardTemplateEntity) {
        updateReward(reward.copy(position = rewardPosition(reward.id) ?: reward.position))
    }

    @Transaction
    open suspend fun reorderRewards(ids: List<String>) {
        rewardIds().reorderedBy(ids) { it }.forEachIndexed { index, id -> setRewardPosition(id, index) }
    }

    @Query("DELETE FROM reward_templates WHERE id = :id")
    abstract suspend fun deleteReward(id: String)

    @Query("SELECT * FROM reward_templates WHERE id = :id")
    abstract suspend fun rewardById(id: String): RewardTemplateEntity?

    @Query("SELECT * FROM redemptions ORDER BY rowid DESC")
    abstract fun redemptions(): Flow<List<RedemptionEntity>>

    @Insert
    abstract suspend fun insertRedemption(redemption: RedemptionEntity)

    @Query("SELECT COUNT(*) FROM redemptions WHERE id = :id")
    abstract suspend fun redemptionCount(id: String): Int

    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger")
    abstract suspend fun balance(): Int

    /**
     * Buys a reward in one transaction: refused (false) if the redemption id is already used, the reward is
     * unknown, or paying would pass the debt limit. SQLite has a single writer, so the balance read and
     * the writes cannot interleave with another spend.
     */
    @Transaction
    open suspend fun redeemReward(rewardId: String, redemptionId: String, ledgerId: String, at: Long): Boolean {
        if (redemptionCount(redemptionId) > 0) return false
        val reward = rewardById(rewardId) ?: return false
        val price = RewardRules.price(RewardTier.fromLevel(reward.tier), reward.customPrice)
        if (!CoinRules.canPay(balance(), price)) return false
        insertRedemption(
            RedemptionEntity(
                redemptionId, reward.id, reward.name, reward.tier, reward.energy, price, at,
                RedemptionSource.Purchase.name, null,
            ),
        )
        insertLedger(LedgerEntity(ledgerId, -price, at, LedgerReason.RewardRedeemed.name, redemptionId))
        return true
    }

    @Query("SELECT * FROM reward_templates ORDER BY position, rowid")
    abstract suspend fun allRewards(): List<RewardTemplateEntity>

    /**
     * Rolls for a reward in one transaction: refused (null) if the tier cannot be rolled, the redemption id is
     * used, paying would pass the debt limit, or the tier has no rewards. The balance read, the random
     * choice and the writes share the transaction, so no other spend can slip in between.
     */
    @Transaction
    open suspend fun mysteryRoll(
        tier: RewardTier,
        redemptionId: String,
        ledgerId: String,
        at: Long,
        random: Random,
    ): RedemptionEntity? {
        if (tier !in RollRules.RollableTiers) return null
        if (redemptionCount(redemptionId) > 0) return null
        val price = RollRules.price(tier)
        if (!CoinRules.canPay(balance(), price)) return null
        val outcome = RollRules.choose(random, tier, allRewards().map { it.toDomain() }) ?: return null
        val reward = outcome.reward
        val redemption = RedemptionEntity(
            redemptionId, reward.id, reward.name, reward.tier.level, reward.energy.name, price, at,
            RedemptionSource.Roll.name, tier.level,
        )
        insertRedemption(redemption)
        insertLedger(LedgerEntity(ledgerId, -price, at, LedgerReason.MysteryRoll.name, redemptionId))
        return redemption
    }

    // ---- completion

    @Query(
        """UPDATE planned_tasks SET completedAtMillis = :at WHERE id = :id AND completedAtMillis IS NULL
           AND NOT EXISTS (SELECT 1 FROM planned_subtasks WHERE plannedTaskId = :id)""",
    )
    abstract suspend fun markTaskCompleted(id: String, at: Long): Int

    @Query("SELECT reward FROM planned_tasks WHERE id = :id")
    abstract suspend fun taskReward(id: String): Int

    @Query("UPDATE planned_subtasks SET completedAtMillis = :at WHERE id = :id AND completedAtMillis IS NULL")
    abstract suspend fun markSubtaskCompleted(id: String, at: Long): Int

    @Query("SELECT reward FROM planned_subtasks WHERE id = :id")
    abstract suspend fun subtaskReward(id: String): Int

    @Query(
        """UPDATE planned_tasks SET completedAtMillis = NULL WHERE id = :id AND completedAtMillis IS NOT NULL
           AND NOT EXISTS (SELECT 1 FROM planned_subtasks WHERE plannedTaskId = :id)""",
    )
    abstract suspend fun clearTaskCompletion(id: String): Int

    @Query("UPDATE planned_subtasks SET completedAtMillis = NULL WHERE id = :id AND completedAtMillis IS NOT NULL")
    abstract suspend fun clearSubtaskCompletion(id: String): Int

    /** Everything recorded in the ledger for a task or subtask: its reward, any critical-hit bonus and earlier undos. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM ledger WHERE refId = :refId")
    abstract suspend fun netFor(refId: String): Int

    /** Unchecks a subtask-less task and appends the reversal of its net payout in one transaction; null if nothing changed. */
    @Transaction
    open suspend fun uncompleteTask(id: String, at: Long, ledgerId: String): Int? {
        if (clearTaskCompletion(id) == 0) return null
        return reverse(id, at, ledgerId)
    }

    /** Unchecks a subtask with the same reversal; null if nothing changed. */
    @Transaction
    open suspend fun uncompleteSubtask(id: String, at: Long, ledgerId: String): Int? {
        if (clearSubtaskCompletion(id) == 0) return null
        return reverse(id, at, ledgerId)
    }

    private suspend fun reverse(refId: String, at: Long, ledgerId: String): Int {
        val net = netFor(refId)
        if (net != 0) insertLedger(LedgerEntity(ledgerId, -net, at, LedgerReason.Undone.name, refId))
        return net
    }

    /**
     * Marks a subtask-less task completed and appends its reward in one transaction; null if nothing changed.
     * [criticalHits] is asked only after the task was really completed, and a critical hit appends its bonus
     * entry in the same transaction.
     */
    @Transaction
    open suspend fun completeTask(
        id: String,
        at: Long,
        ledgerId: String,
        bonusLedgerId: String,
        criticalHits: CriticalHits,
    ): CompletionResult? {
        if (markTaskCompleted(id, at) == 0) return null
        val reward = taskReward(id)
        insertLedger(LedgerEntity(ledgerId, reward, at, LedgerReason.TaskCompleted.name, id))
        if (!criticalHits.isCritical()) return CompletionResult(reward)
        val bonus = CriticalRules.bonus(reward)
        insertLedger(LedgerEntity(bonusLedgerId, bonus, at, LedgerReason.CriticalHit.name, id))
        return CompletionResult(reward, bonus)
    }

    /** Marks a subtask completed and appends its share in one transaction, with the same critical hit rule; null if nothing changed. */
    @Transaction
    open suspend fun completeSubtask(
        id: String,
        at: Long,
        ledgerId: String,
        bonusLedgerId: String,
        criticalHits: CriticalHits,
    ): CompletionResult? {
        if (markSubtaskCompleted(id, at) == 0) return null
        val reward = subtaskReward(id)
        insertLedger(LedgerEntity(ledgerId, reward, at, LedgerReason.SubtaskCompleted.name, id))
        if (!criticalHits.isCritical()) return CompletionResult(reward)
        val bonus = CriticalRules.bonus(reward)
        insertLedger(LedgerEntity(bonusLedgerId, bonus, at, LedgerReason.CriticalHit.name, id))
        return CompletionResult(reward, bonus)
    }
}
