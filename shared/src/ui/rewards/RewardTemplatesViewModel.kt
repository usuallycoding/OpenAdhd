package com.usuallycoding.openadhd.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardInputError
import com.usuallycoding.openadhd.domain.RewardRepository
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.moved
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RewardTemplatesViewModel(private val repository: RewardRepository) : ViewModel() {
    val rewards: StateFlow<List<RewardTemplate>> =
        repository.rewards.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Whether there are enough items for the order to be worth changing. */
    val canReorder: StateFlow<Boolean> = rewards.map { it.size >= 2 }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val reorderRequested = MutableStateFlow(false)

    /** Whether the list shows move controls. Ends by itself when fewer than two rewards are left; not saved. */
    val reordering: StateFlow<Boolean> = combine(reorderRequested, rewards) { on, list -> on && list.size >= 2 }
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
        if (validate(name, tier, customPrice) != null) return
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
        fun validate(name: String, tier: RewardTier, customPrice: Int?): RewardInputError? =
            RewardRules.validate(name, tier, customPrice)
    }
}
