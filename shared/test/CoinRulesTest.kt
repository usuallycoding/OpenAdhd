package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.Tier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class CoinRulesTest {
    @Test
    fun `base reward doubles with each tier`() {
        assertEquals(5, CoinRules.baseReward(Tier.One))
        assertEquals(10, CoinRules.baseReward(Tier.Two))
        assertEquals(20, CoinRules.baseReward(Tier.Three))
        assertEquals(40, CoinRules.baseReward(Tier.Four))
    }

    @Test
    fun `tier maps from its level`() {
        assertEquals(Tier.Three, Tier.fromLevel(3))
    }

    @Test
    fun `balance of an empty ledger is zero`() {
        assertEquals(0, CoinRules.balance(emptyList()))
    }

    @Test
    fun `balance sums all entries`() {
        val now = Instant.fromEpochSeconds(0)
        val entries = listOf(
            LedgerEntry("a", 5, now, LedgerReason.TaskCompleted),
            LedgerEntry("b", 40, now, LedgerReason.TaskCompleted),
        )
        assertEquals(45, CoinRules.balance(entries))
    }
}
