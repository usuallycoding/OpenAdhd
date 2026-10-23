package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Redemption
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.newId
import com.usuallycoding.openadhd.domain.toPlannedTask
import com.usuallycoding.openadhd.domain.validated
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
    val rewards: List<RewardTemplate> = emptyList(),
    val redemptions: List<Redemption> = emptyList(),
)

/** Pure state transitions shared by every [OpenAdhdStore] implementation. */
internal class StoreLogic(private val clock: Clock) {
    fun create(
        state: StoreState,
        name: String,
        tier: Tier,
        energy: Energy,
        description: String,
        subtasks: List<String>,
    ): Pair<StoreState, TaskTemplate> {
        val template = TaskTemplate(newId(), name, tier, energy, description, subtasks).validated()
        return state.copy(templates = state.templates + template) to template
    }

    fun update(state: StoreState, template: TaskTemplate): StoreState {
        val valid = template.validated()
        return state.copy(templates = state.templates.map { if (it.id == valid.id) valid else it })
    }

    fun delete(state: StoreState, id: String): StoreState =
        state.copy(templates = state.templates.filterNot { it.id == id })

    fun assign(state: StoreState, template: TaskTemplate, date: LocalDate): Pair<StoreState, PlannedTask> {
        val task = template.toPlannedTask(date)
        return state.copy(planned = state.planned + task) to task
    }

    fun remove(state: StoreState, id: String): StoreState =
        state.copy(planned = state.planned.filterNot { it.id == id && !it.hasProgress })

    /** Returns the new state, or null when the entry is unknown, has subtasks, or is already completed. */
    fun completeTask(state: StoreState, id: String): StoreState? {
        val task = state.planned.firstOrNull { it.id == id } ?: return null
        if (task.subtasks.isNotEmpty() || task.isCompleted) return null
        val now = clock.now()
        val reward = LedgerEntry(newId(), task.reward, now, LedgerReason.TaskCompleted, task.id)
        return state.copy(
            planned = state.planned.map { if (it.id == id) it.copy(completedAt = now) else it },
            ledger = state.ledger + reward,
        )
    }

    fun createReward(
        state: StoreState,
        name: String,
        tier: RewardTier,
        energy: Energy,
        description: String,
        customPrice: Int?,
    ): Pair<StoreState, RewardTemplate> {
        val reward = RewardTemplate(newId(), name, tier, energy, description, customPrice).validated()
        return state.copy(rewards = state.rewards + reward) to reward
    }

    fun updateReward(state: StoreState, reward: RewardTemplate): StoreState {
        val valid = reward.validated()
        return state.copy(rewards = state.rewards.map { if (it.id == valid.id) valid else it })
    }

    fun deleteReward(state: StoreState, id: String): StoreState =
        state.copy(rewards = state.rewards.filterNot { it.id == id })

    /** Returns the new state, or null when the reward is unknown, the balance is too low, or the id was used. */
    fun redeem(state: StoreState, rewardId: String, redemptionId: String): StoreState? {
        if (state.redemptions.any { it.id == redemptionId }) return null
        val reward = state.rewards.firstOrNull { it.id == rewardId } ?: return null
        val price = reward.price
        if (CoinRules.balance(state.ledger) < price) return null
        val now = clock.now()
        val redemption = Redemption(redemptionId, reward.id, reward.name, reward.tier, reward.energy, price, now)
        val spend = LedgerEntry(newId(), -price, now, LedgerReason.RewardRedeemed, redemptionId)
        return state.copy(redemptions = state.redemptions + redemption, ledger = state.ledger + spend)
    }

    /** Returns the new state, or null when the subtask is unknown or already completed. */
    fun completeSubtask(state: StoreState, id: String): StoreState? {
        val task = state.planned.firstOrNull { entry -> entry.subtasks.any { it.id == id } } ?: return null
        val subtask = task.subtasks.first { it.id == id }
        if (subtask.isCompleted) return null
        val now = clock.now()
        val reward = LedgerEntry(newId(), subtask.reward, now, LedgerReason.SubtaskCompleted, subtask.id)
        return state.copy(
            planned = state.planned.map { entry ->
                if (entry.id != task.id) entry
                else entry.copy(subtasks = entry.subtasks.map { if (it.id == id) it.copy(completedAt = now) else it })
            },
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

    override suspend fun create(
        name: String,
        tier: Tier,
        energy: Energy,
        description: String,
        subtasks: List<String>,
    ): TaskTemplate {
        // Validate before touching the state so invalid input throws to the caller.
        TaskTemplate("", name, tier, energy, description, subtasks).validated()
        var created: TaskTemplate? = null
        state.update { s ->
            logic.create(s, name, tier, energy, description, subtasks).also { created = it.second }.first
        }
        return created!!
    }

    override suspend fun update(template: TaskTemplate) {
        template.validated()
        state.update { logic.update(it, template) }
    }

    override suspend fun delete(id: String) = state.update { logic.delete(it, id) }

    override suspend fun assign(template: TaskTemplate, date: LocalDate): PlannedTask {
        var assigned: PlannedTask? = null
        state.update { s -> logic.assign(s, template, date).also { assigned = it.second }.first }
        return assigned!!
    }

    override suspend fun remove(id: String) = state.update { logic.remove(it, id) }

    override val rewards: Flow<List<RewardTemplate>> = state.map { it.rewards }
    override val redemptions: Flow<List<Redemption>> = state.map { it.redemptions.asReversed() }

    override suspend fun createReward(
        name: String,
        tier: RewardTier,
        energy: Energy,
        description: String,
        customPrice: Int?,
    ): RewardTemplate {
        // Validate before touching the state so invalid input throws to the caller.
        RewardTemplate("", name, tier, energy, description, customPrice).validated()
        var created: RewardTemplate? = null
        state.update { s ->
            logic.createReward(s, name, tier, energy, description, customPrice).also { created = it.second }.first
        }
        return created!!
    }

    override suspend fun updateReward(reward: RewardTemplate) {
        reward.validated()
        state.update { logic.updateReward(it, reward) }
    }

    override suspend fun deleteReward(id: String) = state.update { logic.deleteReward(it, id) }

    override suspend fun redeem(rewardId: String, redemptionId: String): Boolean =
        transition { logic.redeem(it, rewardId, redemptionId) }

    override suspend fun completeTask(id: String): Boolean = transition { logic.completeTask(it, id) }

    override suspend fun completeSubtask(id: String): Boolean = transition { logic.completeSubtask(it, id) }

    private fun transition(next: (StoreState) -> StoreState?): Boolean {
        while (true) {
            val before = state.value
            val after = next(before) ?: return false
            if (state.compareAndSet(before, after)) return true
        }
    }
}
