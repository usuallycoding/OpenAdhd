package com.usuallycoding.openadhd.data

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.CompletionResult
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.CriticalRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.PlannedTask
import com.usuallycoding.openadhd.domain.Redemption
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.Reminder
import com.usuallycoding.openadhd.domain.GameRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.StoreState
import com.usuallycoding.openadhd.domain.RollResult
import com.usuallycoding.openadhd.domain.SettingCheck
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.checkSetting
import com.usuallycoding.openadhd.domain.RollRules
import com.usuallycoding.openadhd.domain.TaskSchedule
import com.usuallycoding.openadhd.domain.TaskTemplate
import com.usuallycoding.openadhd.domain.TemplateReminder
import com.usuallycoding.openadhd.domain.generatedTask
import com.usuallycoding.openadhd.domain.reorderedBy
import com.usuallycoding.openadhd.domain.Tier
import com.usuallycoding.openadhd.domain.newId
import com.usuallycoding.openadhd.domain.toPlannedTask
import com.usuallycoding.openadhd.domain.validated
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.random.Random
import kotlin.time.Clock

/** Pure state transitions shared by every [OpenAdhdStore] implementation. */
internal class StoreLogic(
    private val clock: Clock,
    private val random: Random = Random.Default,
    private val criticalHits: CriticalHits = CriticalHits.Never,
) {
    /** The reward entry, plus a bonus entry when this completion turns out to be a critical hit. */
    private fun payout(reward: LedgerEntry, refId: String, rules: GameRules): Pair<List<LedgerEntry>, CompletionResult> {
        if (!criticalHits.isCritical(rules.criticalOneIn)) return listOf(reward) to CompletionResult(reward.amount)
        val bonus = LedgerEntry(newId(), CriticalRules.bonus(reward.amount, rules), reward.timestamp, LedgerReason.CriticalHit, refId)
        return listOf(reward, bonus) to CompletionResult(reward.amount, bonus.amount)
    }

    fun create(
        state: StoreState,
        name: String,
        tier: Tier,
        energy: Energy,
        description: String,
        subtasks: List<String>,
        schedule: TaskSchedule = TaskSchedule.None,
        reminder: TemplateReminder? = null,
    ): Pair<StoreState, TaskTemplate> {
        val template = TaskTemplate(newId(), name, tier, energy, description, subtasks, schedule, reminder).validated(state.rules)
        return state.copy(templates = listOf(template) + state.templates) to template
    }

    fun update(state: StoreState, template: TaskTemplate): StoreState {
        val valid = template.validated(state.rules)
        // The stored marker is kept whatever the caller's copy says, so an edit can never make a deleted entry come back.
        return state.copy(templates = state.templates.map { if (it.id == valid.id) valid.copy(lastGenerated = it.lastGenerated) else it })
    }

    /** Adds an entry for each template due on [today] that has not been generated for it yet, and marks them generated. */
    fun generateDue(state: StoreState, today: LocalDate, zone: TimeZone): Pair<StoreState, List<PlannedTask>> {
        val due = state.templates.filter { it.schedule.isDue(today) && it.lastGenerated != today }
        if (due.isEmpty()) return state to emptyList()
        val tasks = due.map { it.generatedTask(today, zone, state.rules) }
        val ids = due.map { it.id }.toSet()
        return state.copy(
            templates = state.templates.map { if (it.id in ids) it.copy(lastGenerated = today) else it },
            planned = state.planned + tasks,
        ) to tasks
    }

    fun delete(state: StoreState, id: String): StoreState =
        state.copy(templates = state.templates.filterNot { it.id == id })

    fun reorderTemplates(state: StoreState, ids: List<String>): StoreState =
        state.copy(templates = state.templates.reorderedBy(ids) { it.id })

    fun reorderRewards(state: StoreState, ids: List<String>): StoreState =
        state.copy(rewards = state.rewards.reorderedBy(ids) { it.id })

    /** Reorders one day's entries within the slots that day already occupies, leaving other days where they are. */
    fun reorderEntries(state: StoreState, date: LocalDate, ids: List<String>): StoreState {
        val reordered = state.planned.filter { it.date == date }.reorderedBy(ids) { it.id }.iterator()
        return state.copy(planned = state.planned.map { if (it.date == date) reordered.next() else it })
    }

    fun assign(state: StoreState, template: TaskTemplate, date: LocalDate): Pair<StoreState, PlannedTask> {
        val task = template.toPlannedTask(date, state.rules)
        return state.copy(planned = state.planned + task) to task
    }

    /** The state with [reminder] on the entry, or null when the entry is unknown or already completed. */
    fun setReminder(state: StoreState, id: String, reminder: Reminder?): StoreState? {
        val entry = state.planned.firstOrNull { it.id == id } ?: return null
        if (entry.isCompleted) return null
        return state.copy(planned = state.planned.map { if (it.id == id) it.copy(reminder = reminder) else it })
    }

    fun remove(state: StoreState, id: String): StoreState =
        state.copy(planned = state.planned.filterNot { it.id == id && !it.hasProgress })

    /** Returns the new state and what was paid, or null when the entry is unknown, has subtasks, or is already completed. */
    fun completeTask(state: StoreState, id: String): Pair<StoreState, CompletionResult>? {
        val task = state.planned.firstOrNull { it.id == id } ?: return null
        if (task.subtasks.isNotEmpty() || task.isCompleted) return null
        val now = clock.now()
        val (entries, result) = payout(LedgerEntry(newId(), task.reward, now, LedgerReason.TaskCompleted, task.id), task.id, state.rules)
        val next = state.copy(
            planned = state.planned.map { if (it.id == id) it.copy(completedAt = now) else it },
            ledger = state.ledger + entries,
        )
        return next to result
    }

    /** The reversal of everything recorded for [refId], or none when it nets to zero. */
    private fun reversal(ledger: List<LedgerEntry>, refId: String): Pair<List<LedgerEntry>, Int> {
        val net = ledger.filter { it.refId == refId }.sumOf { it.amount }
        if (net == 0) return emptyList<LedgerEntry>() to 0
        return listOf(LedgerEntry(newId(), -net, clock.now(), LedgerReason.Undone, refId)) to net
    }

    /** Returns the new state and the coins taken back, or null when the entry is unknown, has subtasks, or is not completed. */
    fun uncompleteTask(state: StoreState, id: String): Pair<StoreState, Int>? {
        val task = state.planned.firstOrNull { it.id == id } ?: return null
        if (task.subtasks.isNotEmpty() || !task.isCompleted) return null
        val (entries, taken) = reversal(state.ledger, id)
        val next = state.copy(
            planned = state.planned.map { if (it.id == id) it.copy(completedAt = null) else it },
            ledger = state.ledger + entries,
        )
        return next to taken
    }

    /** Returns the new state and the coins taken back, or null when the subtask is unknown or not completed. */
    fun uncompleteSubtask(state: StoreState, id: String): Pair<StoreState, Int>? {
        val task = state.planned.firstOrNull { entry -> entry.subtasks.any { it.id == id } } ?: return null
        if (!task.subtasks.first { it.id == id }.isCompleted) return null
        val (entries, taken) = reversal(state.ledger, id)
        val next = state.copy(
            planned = state.planned.map { entry ->
                if (entry.id != task.id) entry
                else entry.copy(subtasks = entry.subtasks.map { if (it.id == id) it.copy(completedAt = null) else it })
            },
            ledger = state.ledger + entries,
        )
        return next to taken
    }

    fun createReward(
        state: StoreState,
        name: String,
        tier: RewardTier,
        energy: Energy,
        description: String,
        customPrice: Int?,
    ): Pair<StoreState, RewardTemplate> {
        val reward = RewardTemplate(newId(), name, tier, energy, description, customPrice).validated(state.rules)
        return state.copy(rewards = listOf(reward) + state.rewards) to reward.priced(state.rules)
    }

    fun updateReward(state: StoreState, reward: RewardTemplate): StoreState {
        val valid = reward.validated(state.rules)
        return state.copy(rewards = state.rewards.map { if (it.id == valid.id) valid else it })
    }

    fun deleteReward(state: StoreState, id: String): StoreState =
        state.copy(rewards = state.rewards.filterNot { it.id == id })

    /** Returns the new state, or null when the reward is unknown, the purchase would pass the debt limit, or the id was used. */
    fun redeem(state: StoreState, rewardId: String, redemptionId: String): StoreState? {
        if (state.redemptions.any { it.id == redemptionId }) return null
        val reward = state.rewards.firstOrNull { it.id == rewardId } ?: return null
        val rules = state.rules
        val price = reward.priced(rules).price
        if (!CoinRules.canPay(CoinRules.balance(state.ledger), price, rules)) return null
        val now = clock.now()
        val redemption = Redemption(
            redemptionId, reward.id, reward.name, reward.tier, reward.energy, price, now,
            description = reward.description,
        )
        val spend = LedgerEntry(newId(), -price, now, LedgerReason.RewardRedeemed, redemptionId)
        return state.copy(redemptions = state.redemptions + redemption, ledger = state.ledger + spend)
    }

    /**
     * Returns the new state and the roll result, or null when the tier cannot be rolled, the id was used, the
     * balance is too low, or the tier has no rewards.
     */
    fun mysteryRoll(state: StoreState, tier: RewardTier, redemptionId: String): Pair<StoreState, RollResult>? {
        if (tier !in RollRules.RollableTiers) return null
        if (state.redemptions.any { it.id == redemptionId }) return null
        val rules = state.rules
        val price = RollRules.price(tier, rules)
        if (!CoinRules.canPay(CoinRules.balance(state.ledger), price, rules)) return null
        val outcome = RollRules.choose(random, tier, state.rewards, rules) ?: return null
        val now = clock.now()
        val reward = outcome.reward
        val redemption = Redemption(
            redemptionId, reward.id, reward.name, reward.tier, reward.energy, price, now,
            RedemptionSource.Roll, tier, reward.description,
        )
        val spend = LedgerEntry(newId(), -price, now, LedgerReason.MysteryRoll, redemptionId)
        val next = state.copy(redemptions = state.redemptions + redemption, ledger = state.ledger + spend)
        return next to RollResult(redemption, outcome.upgraded)
    }

    /** The state with [key] set to [text], or why it is refused. A value equal to the default is stored as no override. */
    fun saveSetting(state: StoreState, key: SettingKey, text: String): Pair<StoreState?, String?> =
        when (val check = checkSetting(key, text, state.settings, state.rewards, state.templates)) {
            is SettingCheck.Refused -> null to check.message
            is SettingCheck.Accepted -> {
                val value = check.settings.text(key)
                val overrides = if (value == Settings.Default.text(key)) state.overrides - key.storageKey
                else state.overrides + (key.storageKey to value)
                state.copy(overrides = overrides) to null
            }
        }

    fun restoreSetting(state: StoreState, key: SettingKey): StoreState =
        state.copy(overrides = state.overrides - key.storageKey)

    /** Returns the new state and what was paid, or null when the subtask is unknown or already completed. */
    fun completeSubtask(state: StoreState, id: String): Pair<StoreState, CompletionResult>? {
        val task = state.planned.firstOrNull { entry -> entry.subtasks.any { it.id == id } } ?: return null
        val subtask = task.subtasks.first { it.id == id }
        if (subtask.isCompleted) return null
        val now = clock.now()
        val (entries, result) = payout(
            LedgerEntry(newId(), subtask.reward, now, LedgerReason.SubtaskCompleted, subtask.id),
            subtask.id,
            state.rules,
        )
        val next = state.copy(
            planned = state.planned.map { entry ->
                if (entry.id != task.id) entry
                else entry.copy(subtasks = entry.subtasks.map { if (it.id == id) it.copy(completedAt = now) else it })
            },
            ledger = state.ledger + entries,
        )
        return next to result
    }
}

class InMemoryStore(
    clock: Clock = Clock.System,
    initial: StoreState = StoreState(),
    random: Random = Random.Default,
    criticalHits: CriticalHits = CriticalHits.Never,
) : OpenAdhdStore {
    private val logic = StoreLogic(clock, random, criticalHits)
    private val state = MutableStateFlow(initial)

    override val settings: Flow<Settings> = state.map { it.settings }.distinctUntilChanged()
    override val templates: Flow<List<TaskTemplate>> = state.map { it.templates }
    override val entries: Flow<List<LedgerEntry>> = state.map { it.ledger }

    override fun entriesFor(date: LocalDate): Flow<List<PlannedTask>> =
        state.map { s -> s.planned.filter { it.date == date } }

    override val daysWithEntries: Flow<Set<LocalDate>> = state.map { s -> s.planned.map { it.date }.toSet() }

    override suspend fun create(
        name: String,
        tier: Tier,
        energy: Energy,
        description: String,
        subtasks: List<String>,
        schedule: TaskSchedule,
        reminder: TemplateReminder?,
    ): TaskTemplate {
        // Validate before touching the state so invalid input throws to the caller.
        TaskTemplate("", name, tier, energy, description, subtasks, schedule, reminder).validated(state.value.rules)
        var created: TaskTemplate? = null
        state.update { s ->
            logic.create(s, name, tier, energy, description, subtasks, schedule, reminder).also { created = it.second }.first
        }
        return created!!
    }

    override suspend fun update(template: TaskTemplate) {
        template.validated(state.value.rules)
        state.update { logic.update(it, template) }
    }

    override suspend fun delete(id: String) = state.update { logic.delete(it, id) }

    override suspend fun assign(template: TaskTemplate, date: LocalDate): PlannedTask {
        var assigned: PlannedTask? = null
        state.update { s -> logic.assign(s, template, date).also { assigned = it.second }.first }
        return assigned!!
    }

    override suspend fun generateDue(today: LocalDate, zone: TimeZone): List<PlannedTask> {
        var generated: List<PlannedTask> = emptyList()
        state.update { s -> logic.generateDue(s, today, zone).also { generated = it.second }.first }
        return generated
    }

    override suspend fun remove(id: String) = state.update { logic.remove(it, id) }

    override suspend fun setReminder(entryId: String, reminder: Reminder?): Boolean =
        transition { logic.setReminder(it, entryId, reminder) }

    override suspend fun reorderEntries(date: LocalDate, ids: List<String>) =
        state.update { logic.reorderEntries(it, date, ids) }

    override suspend fun reorderTemplates(ids: List<String>) = state.update { logic.reorderTemplates(it, ids) }

    override suspend fun reorderRewards(ids: List<String>) = state.update { logic.reorderRewards(it, ids) }

    override val rewards: Flow<List<RewardTemplate>> = state.map { s -> s.rewards.map { it.priced(s.rules) } }
    override val redemptions: Flow<List<Redemption>> = state.map { it.redemptions.asReversed() }

    override suspend fun createReward(
        name: String,
        tier: RewardTier,
        energy: Energy,
        description: String,
        customPrice: Int?,
    ): RewardTemplate {
        // Validate before touching the state so invalid input throws to the caller.
        RewardTemplate("", name, tier, energy, description, customPrice).validated(state.value.rules)
        var created: RewardTemplate? = null
        state.update { s ->
            logic.createReward(s, name, tier, energy, description, customPrice).also { created = it.second }.first
        }
        return created!!
    }

    override suspend fun updateReward(reward: RewardTemplate) {
        reward.validated(state.value.rules)
        state.update { logic.updateReward(it, reward) }
    }

    override suspend fun deleteReward(id: String) = state.update { logic.deleteReward(it, id) }

    override suspend fun redeem(rewardId: String, redemptionId: String): Boolean =
        transition { logic.redeem(it, rewardId, redemptionId) }

    override suspend fun mysteryRoll(tier: RewardTier, redemptionId: String): RollResult? {
        while (true) {
            val before = state.value
            val (after, result) = logic.mysteryRoll(before, tier, redemptionId) ?: return null
            if (state.compareAndSet(before, after)) return result
        }
    }

    override suspend fun exportData(): StoreState {
        val current = state.value
        return current.copy(rewards = current.rewards.map { it.priced(current.rules) }, overrides = current.settings.overrides)
    }

    override suspend fun importData(data: StoreState) {
        state.value = data.copy(overrides = data.settings.overrides)
    }

    override suspend fun deleteAllData() {
        state.value = StoreState()
    }

    override suspend fun saveSetting(key: SettingKey, text: String): String? {
        while (true) {
            val before = state.value
            val (after, problem) = logic.saveSetting(before, key, text)
            if (after == null) return problem
            if (state.compareAndSet(before, after)) return null
        }
    }

    override suspend fun restoreSetting(key: SettingKey) = state.update { logic.restoreSetting(it, key) }

    override suspend fun completeTask(id: String): CompletionResult? = completion { logic.completeTask(it, id) }

    override suspend fun completeSubtask(id: String): CompletionResult? = completion { logic.completeSubtask(it, id) }

    override suspend fun uncompleteTask(id: String): Int? = undo { logic.uncompleteTask(it, id) }

    override suspend fun uncompleteSubtask(id: String): Int? = undo { logic.uncompleteSubtask(it, id) }

    private fun undo(next: (StoreState) -> Pair<StoreState, Int>?): Int? {
        while (true) {
            val before = state.value
            val (after, taken) = next(before) ?: return null
            if (state.compareAndSet(before, after)) return taken
        }
    }

    private fun completion(next: (StoreState) -> Pair<StoreState, CompletionResult>?): CompletionResult? {
        while (true) {
            val before = state.value
            val (after, result) = next(before) ?: return null
            if (state.compareAndSet(before, after)) return result
        }
    }

    private fun transition(next: (StoreState) -> StoreState?): Boolean {
        while (true) {
            val before = state.value
            val after = next(before) ?: return false
            if (state.compareAndSet(before, after)) return true
        }
    }
}
