package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.LedgerEntry
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.Tier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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

    @Test
    fun `a price can be paid while the balance after paying stays within the debt limit`() {
        assertEquals(500, CoinRules.DebtLimit)
        assertTrue(CoinRules.canPay(balance = 100, price = 50))
        assertTrue(CoinRules.canPay(balance = 30, price = 50))
        assertTrue(CoinRules.canPay(balance = 0, price = 500))
        assertTrue(CoinRules.canPay(balance = -450, price = 50))
        assertFalse(CoinRules.canPay(balance = -451, price = 50))
        assertFalse(CoinRules.canPay(balance = 0, price = 501))
        assertFalse(CoinRules.canPay(balance = -600, price = 1))
    }

    @Test
    fun `coins needed counts the room the debt limit gives`() {
        assertEquals(0, CoinRules.coinsNeeded(balance = 30, price = 50))
        assertEquals(0, CoinRules.coinsNeeded(balance = -450, price = 50))
        assertEquals(1, CoinRules.coinsNeeded(balance = -451, price = 50))
        assertEquals(30, CoinRules.coinsNeeded(balance = -480, price = 50))
        assertEquals(401, CoinRules.coinsNeeded(balance = 0, price = 901))
    }

    @Test
    fun `debt after a purchase is only what goes below zero`() {
        assertEquals(0, CoinRules.debtAfter(balance = 80, price = 50))
        assertEquals(0, CoinRules.debtAfter(balance = 50, price = 50))
        assertEquals(20, CoinRules.debtAfter(balance = 30, price = 50))
        assertEquals(70, CoinRules.debtAfter(balance = -20, price = 50))
    }
}
