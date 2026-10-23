package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.InMemoryStore
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import kotlin.random.Random
import kotlin.time.Clock

class InMemoryStoreTest : StoreContractTest() {
    override fun newStore(clock: Clock, random: Random, criticalHits: CriticalHits): OpenAdhdStore =
        InMemoryStore(clock, random = random, criticalHits = criticalHits)
}
