package com.usuallycoding.openadhd.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.GameRules
import com.usuallycoding.openadhd.domain.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import com.usuallycoding.openadhd.domain.RewardInputError
import com.usuallycoding.openadhd.domain.RewardRepository
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.matchingRewards
import com.usuallycoding.openadhd.domain.moved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RewardTemplatesViewModel(
    private val repository: RewardRepository,
    settings: Flow<Settings> = flowOf(Settings.Default),
) : ViewModel() {
    private val rules: StateFlow<GameRules> =
        settings.map { it.rules }.stateIn(viewModelScope, SharingStarted.Eagerly, GameRules.Default)

    val rewards: StateFlow<List<RewardTemplate>> =
        repository.rewards.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _query = MutableStateFlow("")

    /** The text of the search above the list; kept while the app runs and not saved. */
    val query: StateFlow<String> = _query

    /** The rewards whose name or description contain the search text, in the list's order. */
    val visible: StateFlow<List<RewardTemplate>> =
        combine(rewards, _query) { list, text -> list.matchingRewards(text) }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Changes the search text. Typing turns Reorder off: moving rows in a filtered list would be confusing. */
    fun setQuery(text: String) {
        _query.value = text
        if (text.isNotBlank()) reorderRequested.value = false
    }

    /** Whether there are enough items for the order to be worth changing, and no search is narrowing the list. */
    val canReorder: StateFlow<Boolean> = combine(rewards, _query) { list, text -> list.size >= 2 && text.isBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val reorderRequested = MutableStateFlow(false)

    /** Whether the list shows move controls. Ends by itself when fewer than two rewards are left or a search starts; not saved. */
    val reordering: StateFlow<Boolean> = combine(reorderRequested, rewards, _query) { on, list, text -> on && list.size >= 2 && text.isBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setReordering(on: Boolean) {
        reorderRequested.value = on
    }

    fun moveUp(id: String) = move(id, -1)

    fun moveDown(id: String) = move(id, 1)

    private fun move(id: String, offset: Int) {
        val ids = rewards.value.map { it.id }
        val moved = ids.moved(id, offset)
        if (moved != ids) viewModelScope.launch { repository.reorderRewards(moved) }
    }

    /**
     * Creates a reward, or updates the one with [id]. Invalid input is ignored; check [validate] first so
     * the user can be told what is wrong.
     */
    fun save(
        id: String?,
        name: String,
        tier: RewardTier,
        energy: Energy,
        description: String = "",
        customPrice: Int? = null,
    ) {
        if (validate(name, tier, customPrice, rules.value) != null) return
        // A new reward goes to the top and may not match the search, so the search is cleared to keep it in view.
        if (id == null) _query.value = ""
        viewModelScope.launch {
            if (id == null) {
                repository.createReward(name, tier, energy, description, customPrice)
            } else {
                repository.updateReward(RewardTemplate(id, name, tier, energy, description, customPrice))
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteReward(id) }
    }

    companion object {
        fun validate(name: String, tier: RewardTier, customPrice: Int?, rules: GameRules = GameRules.Default): RewardInputError? =
            RewardRules.validate(name, tier, customPrice, rules)
    }
}
