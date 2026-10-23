package com.usuallycoding.openadhd.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.LedgerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class WalletViewModel(ledger: LedgerRepository) : ViewModel() {
    val balance: StateFlow<Int> =
        ledger.entries.map(CoinRules::balance).stateIn(viewModelScope, SharingStarted.Eagerly, 0)
}
