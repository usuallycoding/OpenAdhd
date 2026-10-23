package com.usuallycoding.openadhd.data.room

import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.newId
import com.usuallycoding.openadhd.domain.requireValidName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

class RoomStore(
    private val database: OpenAdhdDatabase,
    private val clock: Clock = Clock.System,
) : OpenAdhdStore {
    private val dao = database.dao()

    override val templates: Flow<List<TaskTemplate>> = dao.templates().map { list -> list.map { it.toDomain() } }
    override val entries: Flow<List<LedgerEntry>> = dao.ledger().map { list -> list.map { it.toDomain() } }

    override fun entriesFor(date: LocalDate): Flow<List<PlannedTask>> =
        dao.plannedFor(date.toString()).map { list -> list.map { it.toDomain() } }

    override suspend fun create(name: String, tier: Tier, energy: Energy): TaskTemplate {
        val template = TaskTemplate(newId(), requireValidName(name), tier, energy)
        dao.insertTemplate(template.toEntity())
        return template
    }

    override suspend fun update(template: TaskTemplate) {
        dao.updateTemplate(template.copy(name = requireValidName(template.name)).toEntity())
    }

    override suspend fun delete(id: String) = dao.deleteTemplate(id)

    override suspend fun assign(template: TaskTemplate, date: LocalDate): PlannedTask {
        val task = PlannedTask(newId(), date, template.name, template.tier, template.energy, template.id)
        dao.insertPlanned(task.toEntity())
        return task
    }

    override suspend fun remove(id: String) = dao.removePending(id)

    override suspend fun complete(id: String): Boolean =
        dao.completeTask(id, clock.now().toEpochMilliseconds(), newId())
}
