package com.usuallycoding.openadhd.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.flow.Flow

@Dao
abstract class OpenAdhdDao {
    @Query("SELECT * FROM templates ORDER BY rowid")
    abstract fun templates(): Flow<List<TemplateEntity>>

    @Insert
    abstract suspend fun insertTemplate(template: TemplateEntity)

    @Update
    abstract suspend fun updateTemplate(template: TemplateEntity)

    @Query("DELETE FROM templates WHERE id = :id")
    abstract suspend fun deleteTemplate(id: String)

    @Query("SELECT * FROM planned_tasks WHERE date = :date ORDER BY rowid")
    abstract fun plannedFor(date: String): Flow<List<PlannedTaskEntity>>

    @Insert
    abstract suspend fun insertPlanned(task: PlannedTaskEntity)

    @Query("DELETE FROM planned_tasks WHERE id = :id AND completedAtMillis IS NULL")
    abstract suspend fun removePending(id: String)

    @Query("SELECT * FROM planned_tasks WHERE id = :id")
    abstract suspend fun plannedById(id: String): PlannedTaskEntity?

    @Query("UPDATE planned_tasks SET completedAtMillis = :at WHERE id = :id AND completedAtMillis IS NULL")
    abstract suspend fun markCompleted(id: String, at: Long): Int

    @Query("SELECT * FROM ledger ORDER BY rowid")
    abstract fun ledger(): Flow<List<LedgerEntity>>

    @Insert
    abstract suspend fun insertLedger(entry: LedgerEntity)

    /** Marks the entry completed and appends its reward in one transaction; false if nothing changed. */
    @Transaction
    open suspend fun completeTask(id: String, at: Long, ledgerId: String): Boolean {
        val task = plannedById(id) ?: return false
        if (markCompleted(id, at) == 0) return false
        val reward = CoinRules.baseReward(Tier.fromLevel(task.tier))
        insertLedger(LedgerEntity(ledgerId, reward, at, LedgerReason.TaskCompleted.name, id))
        return true
    }
}
