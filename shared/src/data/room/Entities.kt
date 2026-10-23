package com.usuallycoding.openadhd.data.room

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.PlannedSubtask
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Redemption
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tier: Int,
    val energy: String,
    @ColumnInfo(defaultValue = "''") val description: String,
)

@Entity(tableName = "template_subtasks")
data class TemplateSubtaskEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val position: Int,
    val title: String,
)

@Entity(tableName = "planned_tasks")
data class PlannedTaskEntity(
    @PrimaryKey val id: String,
    val date: String,
    val name: String,
    val tier: Int,
    val energy: String,
    val templateId: String?,
    val completedAtMillis: Long?,
    @ColumnInfo(defaultValue = "''") val description: String,
    @ColumnInfo(defaultValue = "0") val reward: Int,
)

@Entity(tableName = "planned_subtasks")
data class PlannedSubtaskEntity(
    @PrimaryKey val id: String,
    val plannedTaskId: String,
    val position: Int,
    val title: String,
    val reward: Int,
    val completedAtMillis: Long?,
)

@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey val id: String,
    val amount: Int,
    val timestampMillis: Long,
    val reason: String,
    val refId: String?,
)

@Entity(tableName = "reward_templates")
data class RewardTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tier: Int,
    val energy: String,
    val description: String,
    val customPrice: Int?,
)

@Entity(tableName = "redemptions")
data class RedemptionEntity(
    @PrimaryKey val id: String,
    val templateId: String?,
    val name: String,
    val tier: Int,
    val energy: String,
    val price: Int,
    val redeemedAtMillis: Long,
    @ColumnInfo(defaultValue = "'Purchase'") val source: String,
    val rolledTier: Int?,
)

data class TemplateWithSubtasks(
    @Embedded val template: TemplateEntity,
    @Relation(parentColumn = "id", entityColumn = "templateId") val subtasks: List<TemplateSubtaskEntity>,
)

data class PlannedWithSubtasks(
    @Embedded val task: PlannedTaskEntity,
    @Relation(parentColumn = "id", entityColumn = "plannedTaskId") val subtasks: List<PlannedSubtaskEntity>,
)

// Relation queries do not guarantee order, so subtasks are sorted by their stored position.
internal fun TemplateWithSubtasks.toDomain() = TaskTemplate(
    id = template.id,
    name = template.name,
    tier = Tier.fromLevel(template.tier),
    energy = Energy.valueOf(template.energy),
    description = template.description,
    subtasks = subtasks.sortedBy { it.position }.map { it.title },
)

internal fun TaskTemplate.toEntity() = TemplateEntity(id, name, tier.level, energy.name, description)

internal fun TaskTemplate.subtaskEntities() =
    subtasks.mapIndexed { index, title -> TemplateSubtaskEntity(newSubtaskId(id, index), id, index, title) }

private fun newSubtaskId(parentId: String, index: Int) = "$parentId#$index"

internal fun PlannedWithSubtasks.toDomain() = PlannedTask(
    id = task.id,
    date = LocalDate.parse(task.date),
    name = task.name,
    tier = Tier.fromLevel(task.tier),
    energy = Energy.valueOf(task.energy),
    templateId = task.templateId,
    completedAt = task.completedAtMillis?.let(Instant::fromEpochMilliseconds),
    description = task.description,
    reward = task.reward,
    subtasks = subtasks.sortedBy { it.position }.map {
        PlannedSubtask(it.id, it.title, it.reward, it.completedAtMillis?.let(Instant::fromEpochMilliseconds))
    },
)

internal fun PlannedTask.toEntity() = PlannedTaskEntity(
    id, date.toString(), name, tier.level, energy.name, templateId,
    completedAt?.toEpochMilliseconds(), description, reward,
)

internal fun PlannedTask.subtaskEntities() = subtasks.mapIndexed { index, subtask ->
    PlannedSubtaskEntity(subtask.id, id, index, subtask.title, subtask.reward, subtask.completedAt?.toEpochMilliseconds())
}

internal fun LedgerEntity.toDomain() = LedgerEntry(
    id, amount, Instant.fromEpochMilliseconds(timestampMillis), LedgerReason.valueOf(reason), refId,
)

internal fun LedgerEntry.toEntity() = LedgerEntity(id, amount, timestamp.toEpochMilliseconds(), reason.name, refId)

internal fun RewardTemplateEntity.toDomain() = RewardTemplate(
    id, name, RewardTier.fromLevel(tier), Energy.valueOf(energy), description, customPrice,
)

internal fun RewardTemplate.toEntity() = RewardTemplateEntity(id, name, tier.level, energy.name, description, customPrice)

internal fun RedemptionEntity.toDomain() = Redemption(
    id, templateId, name, RewardTier.fromLevel(tier), Energy.valueOf(energy), price,
    Instant.fromEpochMilliseconds(redeemedAtMillis),
    RedemptionSource.valueOf(source), rolledTier?.let(RewardTier::fromLevel),
)
