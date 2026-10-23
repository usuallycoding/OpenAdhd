package com.usuallycoding.openadhd.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.Redemption
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

    /** Purchases, newest first. */
    val history: StateFlow<List<Redemption>> =
        store.redemptions.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Buys a reward. The caller creates [redemptionId] when the confirmation opens, so confirming twice in
     * a row records only one purchase.
     */
    fun redeem(rewardId: String, redemptionId: String) {
        viewModelScope.launch { store.redeem(rewardId, redemptionId) }
    }
}
