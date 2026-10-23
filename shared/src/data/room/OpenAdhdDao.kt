package com.usuallycoding.openadhd.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTier
import kotlinx.coroutines.flow.Flow

@Dao
abstract class OpenAdhdDao {
    // ---- templates

    @Transaction
    @Query("SELECT * FROM templates ORDER BY rowid")
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

    @Transaction
    open suspend fun insertTemplateWithSubtasks(template: TemplateEntity, subtasks: List<TemplateSubtaskEntity>) {
        insertTemplate(template)
        insertTemplateSubtasks(subtasks)
    }

    @Transaction
    open suspend fun updateTemplateWithSubtasks(template: TemplateEntity, subtasks: List<TemplateSubtaskEntity>) {
        updateTemplate(template)
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
    @Query("SELECT * FROM planned_tasks WHERE date = :date ORDER BY rowid")
    abstract fun plannedFor(date: String): Flow<List<PlannedWithSubtasks>>

    @Insert
    abstract suspend fun insertPlanned(task: PlannedTaskEntity)

    @Insert
    abstract suspend fun insertPlannedSubtasks(subtasks: List<PlannedSubtaskEntity>)

    @Transaction
    open suspend fun insertPlannedWithSubtasks(task: PlannedTaskEntity, subtasks: List<PlannedSubtaskEntity>) {
        insertPlanned(task)
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

    @Query("SELECT * FROM reward_templates ORDER BY rowid")
    abstract fun rewards(): Flow<List<RewardTemplateEntity>>

    @Insert
    abstract suspend fun insertReward(reward: RewardTemplateEntity)

    @Update
    abstract suspend fun updateReward(reward: RewardTemplateEntity)

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
     * unknown, or the balance is lower than the price. SQLite has a single writer, so the balance read and
     * the writes cannot interleave with another spend.
     */
    @Transaction
    open suspend fun redeemReward(rewardId: String, redemptionId: String, ledgerId: String, at: Long): Boolean {
        if (redemptionCount(redemptionId) > 0) return false
        val reward = rewardById(rewardId) ?: return false
        val price = RewardRules.price(RewardTier.fromLevel(reward.tier), reward.customPrice)
        if (balance() < price) return false
        insertRedemption(RedemptionEntity(redemptionId, reward.id, reward.name, reward.tier, reward.energy, price, at))
        insertLedger(LedgerEntity(ledgerId, -price, at, LedgerReason.RewardRedeemed.name, redemptionId))
        return true
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

    /** Marks a subtask-less task completed and appends its reward in one transaction; false if nothing changed. */
    @Transaction
    open suspend fun completeTask(id: String, at: Long, ledgerId: String): Boolean {
        if (markTaskCompleted(id, at) == 0) return false
        insertLedger(LedgerEntity(ledgerId, taskReward(id), at, LedgerReason.TaskCompleted.name, id))
        return true
    }

    /** Marks a subtask completed and appends its share in one transaction; false if nothing changed. */
    @Transaction
    open suspend fun completeSubtask(id: String, at: Long, ledgerId: String): Boolean {
        if (markSubtaskCompleted(id, at) == 0) return false
        insertLedger(LedgerEntity(ledgerId, subtaskReward(id), at, LedgerReason.SubtaskCompleted.name, id))
        return true
    }
}
