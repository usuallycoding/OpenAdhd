package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.Tier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
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

    @Test
    fun `an even split gives every subtask the same share`() {
        assertEquals(listOf(10, 10, 10, 10), CoinRules.splitPool(40, 4))
    }

    @Test
    fun `an uneven split gives leftover coins to the first subtasks`() {
        assertEquals(listOf(7, 7, 6), CoinRules.splitPool(20, 3))
        assertEquals(listOf(4, 3, 3), CoinRules.splitPool(10, 3))
    }

    @Test
    fun `a single subtask gets the whole pool`() {
        assertEquals(listOf(20), CoinRules.splitPool(20, 1))
    }

    @Test
    fun `a pool equal to the number of subtasks gives one coin each`() {
        assertEquals(List(5) { 1 }, CoinRules.splitPool(5, 5))
    }

    @Test
    fun `a pool cannot be split into more parts than coins`() {
        assertFailsWith<IllegalArgumentException> { CoinRules.splitPool(5, 6) }
        assertFailsWith<IllegalArgumentException> { CoinRules.splitPool(5, 8) }
    }

    @Test
    fun `the subtask limit is the tier's pool capped at the ceiling`() {
        assertEquals(5, CoinRules.maxSubtasks(Tier.One))
        assertEquals(10, CoinRules.maxSubtasks(Tier.Two))
        assertEquals(10, CoinRules.maxSubtasks(Tier.Three))
        assertEquals(10, CoinRules.maxSubtasks(Tier.Four))
        assertEquals(10, CoinRules.SubtaskCeiling)
    }

    @Test
    fun `every share is at least one and shares add up to the pool for every allowed part count`() {
        for (tier in Tier.entries) {
            val pool = CoinRules.baseReward(tier)
            for (parts in 1..CoinRules.maxSubtasks(tier)) {
                val shares = CoinRules.splitPool(pool, parts)
                assertEquals(parts, shares.size)
                assertTrue(shares.all { it >= 1 }, "tier ${tier.level}, $parts parts")
                assertEquals(pool, shares.sum(), "tier ${tier.level}, $parts parts")
            }
        }
    }

    @Test
    fun `a pool cannot be split into zero parts`() {
        assertFailsWith<IllegalArgumentException> { CoinRules.splitPool(10, 0) }
    }
}
