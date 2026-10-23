package com.usuallycoding.openadhd.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.PlannedTask
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
)

@Entity(tableName = "ledger")
data class LedgerEntity(
    @PrimaryKey val id: String,
    val amount: Int,
    val timestampMillis: Long,
    val reason: String,
    val refId: String?,
)

internal fun TemplateEntity.toDomain() = TaskTemplate(id, name, Tier.fromLevel(tier), Energy.valueOf(energy))
internal fun TaskTemplate.toEntity() = TemplateEntity(id, name, tier.level, energy.name)

internal fun PlannedTaskEntity.toDomain() = PlannedTask(
    id = id,
    date = LocalDate.parse(date),
    name = name,
    tier = Tier.fromLevel(tier),
    energy = Energy.valueOf(energy),
    templateId = templateId,
    completedAt = completedAtMillis?.let(Instant::fromEpochMilliseconds),
)

internal fun PlannedTask.toEntity() = PlannedTaskEntity(
    id, date.toString(), name, tier.level, energy.name, templateId, completedAt?.toEpochMilliseconds(),
)

internal fun LedgerEntity.toDomain() = LedgerEntry(
    id, amount, Instant.fromEpochMilliseconds(timestampMillis), LedgerReason.valueOf(reason), refId,
)

internal fun LedgerEntry.toEntity() = LedgerEntity(id, amount, timestamp.toEpochMilliseconds(), reason.name, refId)
