package com.usuallycoding.openadhd.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardInputError
import com.usuallycoding.openadhd.domain.RewardRepository
import com.usuallycoding.openadhd.domain.RewardRules
import com.usuallycoding.openadhd.domain.RewardTemplate
import com.usuallycoding.openadhd.domain.RewardTier
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RewardTemplatesViewModel(private val repository: RewardRepository) : ViewModel() {
    val rewards: StateFlow<List<RewardTemplate>> =
        repository.rewards.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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
