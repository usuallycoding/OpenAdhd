package com.usuallycoding.openadhd.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.RollResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShopViewModel(private val store: OpenAdhdStore) : ViewModel() {
    val balance: StateFlow<Int> =
        store.entries.map(CoinRules::balance).stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val shelves: StateFlow<List<ShopShelf>> =
        combine(store.rewards, balance, ::buildShelves).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val rollOptions: StateFlow<List<RollOption>> =
        combine(store.rewards, balance, ::buildRollOptions).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _lastRoll = MutableStateFlow<RollResult?>(null)

    /** The result of the latest roll, shown in the reveal until [dismissRoll] is called. */
    val lastRoll: StateFlow<RollResult?> = _lastRoll

    /**
     * Buys a reward. The caller creates [redemptionId] when the confirmation opens, so confirming twice in
     * a row records only one purchase.
     */
    fun redeem(rewardId: String, redemptionId: String) {
        viewModelScope.launch { store.redeem(rewardId, redemptionId) }
    }

    /**
     * Rolls for a reward of [tier]. The caller creates [rollId] when the confirmation opens, so confirming
     * twice in a row rolls once. A refused roll leaves [lastRoll] unchanged.
     */
    fun roll(tier: RewardTier, rollId: String) {
        viewModelScope.launch { store.mysteryRoll(tier, rollId)?.let { _lastRoll.value = it } }
    }

    fun dismissRoll() {
        _lastRoll.value = null
    }
}
