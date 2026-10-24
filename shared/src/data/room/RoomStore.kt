package com.usuallycoding.openadhd.data.room

import com.usuallycoding.openadhd.domain.CompletionResult
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Redemption
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.RollResult
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.StoreState
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.newId
import com.usuallycoding.openadhd.domain.toPlannedTask
import com.usuallycoding.openadhd.domain.validated
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.time.Clock

class RoomStore(
    private val database: OpenAdhdDatabase,
    private val clock: Clock = Clock.System,
    private val random: Random = Random.Default,
    private val criticalHits: CriticalHits = CriticalHits.Never,
) : OpenAdhdStore {
    private val dao = database.dao()

    override val settings: Flow<Settings> = dao.settingRows().map { it.toSettings() }.distinctUntilChanged()
    override val templates: Flow<List<TaskTemplate>> = dao.templates().map { list -> list.map { it.toDomain() } }
    override val entries: Flow<List<LedgerEntry>> = dao.ledger().map { list -> list.map { it.toDomain() } }

    override fun entriesFor(date: LocalDate): Flow<List<PlannedTask>> =
        dao.plannedFor(date.toString()).map { list -> list.map { it.toDomain() } }

    override val daysWithEntries: Flow<Set<LocalDate>> =
        dao.plannedDates().map { list -> list.map { LocalDate.parse(it) }.toSet() }

    override suspend fun create(
        name: String,
        tier: Tier,
        energy: Energy,
        description: String,
        subtasks: List<String>,
    ): TaskTemplate {
        val template = TaskTemplate(newId(), name, tier, energy, description, subtasks).validated(dao.currentSettings().rules)
        dao.insertTemplateWithSubtasks(template.toEntity(), template.subtaskEntities())
        return template
    }

    override suspend fun update(template: TaskTemplate) {
        val valid = template.validated(dao.currentSettings().rules)
        dao.updateTemplateWithSubtasks(valid.toEntity(), valid.subtaskEntities())
    }

    override suspend fun delete(id: String) = dao.deleteTemplate(id)

    override suspend fun reorderTemplates(ids: List<String>) = dao.reorderTemplates(ids)

    override suspend fun reorderRewards(ids: List<String>) = dao.reorderRewards(ids)

    override suspend fun reorderEntries(date: LocalDate, ids: List<String>) = dao.reorderPlanned(date.toString(), ids)

    override suspend fun assign(template: TaskTemplate, date: LocalDate): PlannedTask {
        val task = template.toPlannedTask(date, dao.currentSettings().rules)
        dao.insertPlannedWithSubtasks(task.toEntity(), task.subtaskEntities())
        return task
    }

    override suspend fun remove(id: String) = dao.removePlanned(id)

    override suspend fun setReminder(entryId: String, reminder: Reminder?): Boolean = dao.setReminder(entryId, reminder)

    override val rewards: Flow<List<RewardTemplate>> =
        combine(dao.rewards(), settings) { list, settings -> list.map { it.toDomain(settings.rules) } }
    override val redemptions: Flow<List<Redemption>> = dao.redemptions().map { list -> list.map { it.toDomain() } }

    override suspend fun createReward(
        name: String,
        tier: RewardTier,
        energy: Energy,
        description: String,
        customPrice: Int?,
    ): RewardTemplate {
        val reward = RewardTemplate(newId(), name, tier, energy, description, customPrice).validated(dao.currentSettings().rules)
        dao.insertRewardFirst(reward.toEntity())
        return reward
    }

    override suspend fun updateReward(reward: RewardTemplate) {
        dao.updateRewardKeepingPosition(reward.validated(dao.currentSettings().rules).toEntity())
    }

    override suspend fun deleteReward(id: String) = dao.deleteReward(id)

    override suspend fun redeem(rewardId: String, redemptionId: String): Boolean =
        dao.redeemReward(rewardId, redemptionId, newId(), clock.now().toEpochMilliseconds())

    override suspend fun mysteryRoll(tier: RewardTier, redemptionId: String): RollResult? =
        dao.mysteryRoll(tier, redemptionId, newId(), clock.now().toEpochMilliseconds(), random)?.let { entity ->
            val redemption = entity.toDomain()
            RollResult(redemption, redemption.isLuckyUpgrade)
        }

    override suspend fun exportData(): StoreState = dao.exportAll()

    override suspend fun importData(data: StoreState) = dao.replaceAll(data)

    override suspend fun deleteAllData() = dao.clearAll()

    override suspend fun saveSetting(key: SettingKey, text: String): String? = dao.saveSetting(key, text)

    override suspend fun restoreSetting(key: SettingKey) = dao.deleteSetting(key.storageKey)

    override suspend fun completeTask(id: String): CompletionResult? =
        dao.completeTask(id, clock.now().toEpochMilliseconds(), newId(), newId(), criticalHits)

    override suspend fun completeSubtask(id: String): CompletionResult? =
        dao.completeSubtask(id, clock.now().toEpochMilliseconds(), newId(), newId(), criticalHits)

    override suspend fun uncompleteTask(id: String): Int? =
        dao.uncompleteTask(id, clock.now().toEpochMilliseconds(), newId())

    override suspend fun uncompleteSubtask(id: String): Int? =
        dao.uncompleteSubtask(id, clock.now().toEpochMilliseconds(), newId())
}
