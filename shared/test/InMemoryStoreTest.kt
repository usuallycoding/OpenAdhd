package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import kotlin.time.Clock

class InMemoryStoreTest : StoreContractTest() {
    override fun newStore(clock: Clock): OpenAdhdStore = InMemoryStore(clock)
}
