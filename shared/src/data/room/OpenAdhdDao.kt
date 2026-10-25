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
import com.usuallycoding.openadhd.domain.SettingCheck
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.checkSetting
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.generatedTask
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.StoreState
import com.usuallycoding.openadhd.domain.RollRules
import com.usuallycoding.openadhd.domain.reorderedBy
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

@Dao
abstract class OpenAdhdDao {
    // ---- backup

    @Insert
    abstract suspend fun insertTemplateRows(templates: List<TemplateEntity>)

    @Insert
    abstract suspend fun insertPlannedRows(tasks: List<PlannedTaskEntity>)

    @Insert
    abstract suspend fun insertLedgerRows(entries: List<LedgerEntity>)

    @Insert
    abstract suspend fun insertRewardRows(rewards: List<RewardTemplateEntity>)

    @Insert
    abstract suspend fun insertRedemptionRows(redemptions: List<RedemptionEntity>)

    @Transaction
    @Query("SELECT * FROM planned_tasks ORDER BY date, position, rowid")
    abstract suspend fun plannedOnce(): List<PlannedWithSubtasks>

    @Query("SELECT * FROM ledger ORDER BY rowid")
    abstract suspend fun ledgerOnce(): List<LedgerEntity>

    @Query("SELECT * FROM redemptions ORDER BY rowid")
    abstract suspend fun redemptionsOnce(): List<RedemptionEntity>

    @Query("DELETE FROM templates")
    abstract suspend fun deleteAllTemplates()

    @Query("DELETE FROM template_subtasks")
    abstract suspend fun deleteAllTemplateSubtasks()

    @Query("DELETE FROM planned_tasks")
    abstract suspend fun deleteAllPlanned()

    @Query("DELETE FROM planned_subtasks")
    abstract suspend fun deleteAllPlannedSubtasks()

    @Query("DELETE FROM ledger")
    abstract suspend fun deleteAllLedger()

    @Query("DELETE FROM reward_templates")
    abstract suspend fun deleteAllRewards()

    @Query("DELETE FROM redemptions")
    abstract suspend fun deleteAllRedemptions()

    @Query("DELETE FROM settings")
    abstract suspend fun deleteAllSettings()

    /** Everything the app holds, read in one transaction, with every list in the order the app shows it. */
    @Transaction
    open suspend fun exportAll(): StoreState {
        val settings = currentSettings()
        return StoreState(
            templates = templatesOnce().map { it.toDomain() },
            planned = plannedOnce().map { it.toDomain() },
            ledger = ledgerOnce().map { it.toDomain() },
            rewards = allRewards().map { it.toDomain(settings.rules) },
            redemptions = redemptionsOnce().map { it.toDomain() },
            overrides = settings.overrides,
        )
    }

    /** Removes every row of every table. */
    @Transaction
    open suspend fun clearAll() {
        deleteAllTemplateSubtasks()
        deleteAllTemplates()
        deleteAllPlannedSubtasks()
        deleteAllPlanned()
        deleteAllLedger()
        deleteAllRewards()
        deleteAllRedemptions()
        deleteAllSettings()
    }

    /**
     * Replaces everything with [data] in one transaction, so a failure leaves the old data. Positions are
     * renumbered from the order of each list (per day for Today entries).
     */
    @Transaction
    open suspend fun replaceAll(data: StoreState) {
        clearAll()
        insertTemplateRows(data.templates.mapIndexed { index, template -> template.toEntity().copy(position = index) })
        insertTemplateSubtasks(data.templates.flatMap { it.subtaskEntities() })
        val countPerDay = mutableMapOf<String, Int>()
        insertPlannedRows(
            data.planned.map { task ->
                val position = countPerDay[task.date.toString()] ?: 0
                countPerDay[task.date.toString()] = position + 1
                task.toEntity().copy(position = position)
            },
        )
        insertPlannedSubtasks(data.planned.flatMap { it.subtaskEntities() })
        insertLedgerRows(data.ledger.map { it.toEntity() })
        insertRewardRows(data.rewards.mapIndexed { index, reward -> reward.toEntity().copy(position = index) })
        insertRedemptionRows(data.redemptions.map { it.toEntity() })
        data.settings.overrides.forEach { (key, value) -> upsertSetting(key, value) }
    }

    // ---- settings

    @Query("SELECT * FROM settings")
    abstract fun settingRows(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings")
    abstract suspend fun settingRowsOnce(): List<SettingEntity>

    @Query("INSERT OR REPLACE INTO settings (`key`, `value`) VALUES (:key, :value)")
    abstract suspend fun upsertSetting(key: String, value: String)

    @Query("DELETE FROM settings WHERE `key` = :key")
    abstract suspend fun deleteSetting(key: String)

    @Transaction
    @Query("SELECT * FROM templates ORDER BY position, rowid")
    abstract suspend fun templatesOnce(): List<TemplateWithSubtasks>

    /** The settings in force: the defaults with the saved rows on top. */
    open suspend fun currentSettings(): Settings = settingRowsOnce().toSettings()

    /**
     * Checks and saves a setting in one transaction with the reads it is checked against. A value equal to the
     * default is stored as no row. Returns why it was refused, or null when it was saved.
     */
    @Transaction
    open suspend fun saveSetting(key: SettingKey, text: String): String? {
        val check = checkSetting(
            key, text, currentSettings(),
            allRewards().map { it.toDomain() }, templatesOnce().map { it.toDomain() },
        )
        return when (check) {
            is SettingCheck.Refused -> check.message
            is SettingCheck.Accepted -> {
                val value = check.settings.text(key)
                if (value == Settings.Default.text(key)) deleteSetting(key.storageKey) else upsertSetting(key.storageKey, value)
                null
            }
        }
    }

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

    /** One before the first template, so a new template goes to the top; positions may be negative until the next reorder. */
    @Query("SELECT COALESCE(MIN(position), 1) - 1 FROM templates")
    abstract suspend fun firstTemplatePosition(): Int

    @Query("SELECT position FROM templates WHERE id = :id")
    abstract suspend fun templatePosition(id: String): Int?

    @Query("SELECT lastGenerated FROM templates WHERE id = :id")
    abstract suspend fun templateLastGenerated(id: String): String?

    @Query("UPDATE templates SET lastGenerated = :date WHERE id = :id")
    abstract suspend fun setTemplateLastGenerated(id: String, date: String)

    /** See [com.usuallycoding.openadhd.domain.PlannerRepository.generateDue]; one transaction, so two callers cannot both generate. */
    @Transaction
    open suspend fun generateDue(today: LocalDate, zone: TimeZone): List<PlannedTask> {
        val rules = currentSettings().rules
        val due = templatesOnce().map { it.toDomain() }.filter { it.schedule.isDue(today) && it.lastGenerated != today }
        return due.map { template ->
            val task = template.generatedTask(today, zone, rules)
            insertPlannedWithSubtasks(task.toEntity(), task.subtaskEntities())
            setTemplateLastGenerated(template.id, today.toString())
            task
        }
    }

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
        insertTemplate(template.copy(position = firstTemplatePosition()))
        insertTemplateSubtasks(subtasks)
    }

    @Transaction
    open suspend fun updateTemplateWithSubtasks(template: TemplateEntity, subtasks: List<TemplateSubtaskEntity>) {
        // @Update writes every column, so carry the stored position and generated day over or an edit would move the
        // template and let a deleted entry come back.
        updateTemplate(
            template.copy(
                position = templatePosition(template.id) ?: template.position,
                lastGenerated = templateLastGenerated(template.id),
            ),
        )
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

    @Transaction
    @Query("SELECT * FROM planned_tasks WHERE id = :id")
    abstract suspend fun plannedById(id: String): PlannedWithSubtasks?

    @Query("UPDATE planned_tasks SET reminderAtMillis = :at, reminderRepeatMinutes = :repeatMinutes WHERE id = :id")
    abstract suspend fun updateReminder(id: String, at: Long?, repeatMinutes: Int?)

    /** Sets or clears an entry's reminder; false, changing nothing, when the entry is unknown or already completed. */
    @Transaction
    open suspend fun setReminder(id: String, reminder: Reminder?): Boolean {
        val entry = plannedById(id)?.toDomain() ?: return false
        if (entry.isCompleted) return false
        updateReminder(id, reminder?.startAt?.toEpochMilliseconds(), reminder?.repeatMinutes)
        return true
    }

    @Query("SELECT DISTINCT date FROM planned_tasks")
    abstract fun plannedDates(): Flow<List<String>>

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

    /** One before the first reward, so a new reward goes to the top; positions may be negative until the next reorder. */
    @Query("SELECT COALESCE(MIN(position), 1) - 1 FROM reward_templates")
    abstract suspend fun firstRewardPosition(): Int

    @Query("SELECT position FROM reward_templates WHERE id = :id")
    abstract suspend fun rewardPosition(id: String): Int?

    @Query("SELECT id FROM reward_templates ORDER BY position, rowid")
    abstract suspend fun rewardIds(): List<String>

    @Query("UPDATE reward_templates SET position = :position WHERE id = :id")
    abstract suspend fun setRewardPosition(id: String, position: Int)

    @Transaction
    open suspend fun insertRewardFirst(reward: RewardTemplateEntity) {
        insertReward(reward.copy(position = firstRewardPosition()))
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
        val rules = currentSettings().rules
        val price = RewardRules.price(RewardTier.fromLevel(reward.tier), reward.customPrice, rules)
        if (!CoinRules.canPay(balance(), price, rules)) return false
        insertRedemption(
            RedemptionEntity(
                redemptionId, reward.id, reward.name, reward.tier, reward.energy, price, at,
                RedemptionSource.Purchase.name, null, reward.description,
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
        val rules = currentSettings().rules
        val price = RollRules.price(tier, rules)
        if (!CoinRules.canPay(balance(), price, rules)) return null
        val outcome = RollRules.choose(random, tier, allRewards().map { it.toDomain(rules) }, rules) ?: return null
        val reward = outcome.reward
        val redemption = RedemptionEntity(
            redemptionId, reward.id, reward.name, reward.tier.level, reward.energy.name, price, at,
            RedemptionSource.Roll.name, tier.level, reward.description,
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
        val rules = currentSettings().rules
        if (!criticalHits.isCritical(rules.criticalOneIn)) return CompletionResult(reward)
        val bonus = CriticalRules.bonus(reward, rules)
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
        val rules = currentSettings().rules
        if (!criticalHits.isCritical(rules.criticalOneIn)) return CompletionResult(reward)
        val bonus = CriticalRules.bonus(reward, rules)
        insertLedger(LedgerEntity(bonusLedgerId, bonus, at, LedgerReason.CriticalHit.name, id))
        return CompletionResult(reward, bonus)
    }
}
