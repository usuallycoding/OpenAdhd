package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.newId
import com.usuallycoding.openadhd.domain.requireValidName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** The whole persisted state; also the unit a persistent store reads and writes. */
data class StoreState(
    val templates: List<TaskTemplate> = emptyList(),
    val planned: List<PlannedTask> = emptyList(),
    val ledger: List<LedgerEntry> = emptyList(),
)

/** Pure state transitions shared by every [OpenAdhdStore] implementation. */
internal class StoreLogic(private val clock: Clock) {
    fun create(state: StoreState, name: String, tier: Tier, energy: Energy): Pair<StoreState, TaskTemplate> {
        val template = TaskTemplate(newId(), requireValidName(name), tier, energy)
        return state.copy(templates = state.templates + template) to template
    }

    fun update(state: StoreState, template: TaskTemplate): StoreState {
        val valid = template.copy(name = requireValidName(template.name))
        return state.copy(templates = state.templates.map { if (it.id == valid.id) valid else it })
    }

    fun delete(state: StoreState, id: String): StoreState =
        state.copy(templates = state.templates.filterNot { it.id == id })

    fun assign(state: StoreState, template: TaskTemplate, date: LocalDate): Pair<StoreState, PlannedTask> {
        val task = PlannedTask(newId(), date, template.name, template.tier, template.energy, template.id)
        return state.copy(planned = state.planned + task) to task
    }

    fun remove(state: StoreState, id: String): StoreState =
        state.copy(planned = state.planned.filterNot { it.id == id && !it.isCompleted })

    /** Returns the new state, or null when the entry is unknown or already completed. */
    fun complete(state: StoreState, id: String): StoreState? {
        val task = state.planned.firstOrNull { it.id == id } ?: return null
        if (task.isCompleted) return null
        val now = clock.now()
        val reward = LedgerEntry(newId(), CoinRules.baseReward(task.tier), now, LedgerReason.TaskCompleted, task.id)
        return state.copy(
            planned = state.planned.map { if (it.id == id) it.copy(completedAt = now) else it },
            ledger = state.ledger + reward,
        )
    }
}

class InMemoryStore(
    clock: Clock = Clock.System,
    initial: StoreState = StoreState(),
) : OpenAdhdStore {
    private val logic = StoreLogic(clock)
    private val state = MutableStateFlow(initial)

    override val templates: Flow<List<TaskTemplate>> = state.map { it.templates }
    override val entries: Flow<List<LedgerEntry>> = state.map { it.ledger }

    override fun entriesFor(date: LocalDate): Flow<List<PlannedTask>> =
        state.map { s -> s.planned.filter { it.date == date } }

    override suspend fun create(name: String, tier: Tier, energy: Energy): TaskTemplate {
        var created: TaskTemplate? = null
        requireValidName(name)
        state.update { s -> logic.create(s, name, tier, energy).also { created = it.second }.first }
        return created!!
    }

    override suspend fun update(template: TaskTemplate) {
        requireValidName(template.name)
        state.update { logic.update(it, template) }
    }

    override suspend fun delete(id: String) = state.update { logic.delete(it, id) }

    override suspend fun assign(template: TaskTemplate, date: LocalDate): PlannedTask {
        var assigned: PlannedTask? = null
        state.update { s -> logic.assign(s, template, date).also { assigned = it.second }.first }
        return assigned!!
    }

    override suspend fun remove(id: String) = state.update { logic.remove(it, id) }

    override suspend fun complete(id: String): Boolean {
        while (true) {
            val before = state.value
            val after = logic.complete(before, id) ?: return false
            if (state.compareAndSet(before, after)) return true
        }
    }
}
