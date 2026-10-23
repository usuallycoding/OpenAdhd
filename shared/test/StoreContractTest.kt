package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class FakeClock(var current: Instant = Instant.parse("2026-10-23T10:00:00Z")) : Clock {
    override fun now(): Instant = current
}

/** Behavior every [OpenAdhdStore] implementation must have. */
abstract class StoreContractTest {
    protected val clock = FakeClock()
    protected abstract fun newStore(clock: Clock): OpenAdhdStore

    private val today: LocalDate get() = clock.todayIn(TimeZone.UTC)

    @Test
    fun `blank names are rejected`() = runTest {
        val store = newStore(clock)
        assertFailsWith<IllegalArgumentException> { store.create("   ", Tier.Two, Energy.Medium) }
        assertTrue(store.templates.first().isEmpty())
    }

    @Test
    fun `created templates are listed with their values`() = runTest {
        val store = newStore(clock)
        store.create("  Reply to emails ", Tier.Two, Energy.Medium)
        val template = store.templates.first().single()
        assertEquals("Reply to emails", template.name)
        assertEquals(Tier.Two, template.tier)
        assertEquals(Energy.Medium, template.energy)
    }

    @Test
    fun `editing a template does not change past entries`() = runTest {
        val store = newStore(clock)
        val template = store.create("Laundry", Tier.Two, Energy.Low)
        store.assign(template, today)
        store.update(template.copy(name = "Laundry and fold", tier = Tier.Four))
        assertEquals(Tier.Four, store.templates.first().single().tier)
        val entry = store.entriesFor(today).first().single()
        assertEquals("Laundry", entry.name)
        assertEquals(Tier.Two, entry.tier)
    }

    @Test
    fun `deleting a template keeps its entries and ledger records`() = runTest {
        val store = newStore(clock)
        val template = store.create("Dishes", Tier.One, Energy.Low)
        val entry = store.assign(template, today)
        store.complete(entry.id)
        store.delete(template.id)
        assertTrue(store.templates.first().isEmpty())
        assertEquals(1, store.entriesFor(today).first().size)
        assertEquals(5, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `the same template can be assigned twice`() = runTest {
        val store = newStore(clock)
        val template = store.create("Walk", Tier.One, Energy.Low)
        store.assign(template, today)
        store.assign(template, today)
        assertEquals(2, store.entriesFor(today).first().size)
    }

    @Test
    fun `entries from another day are not in today`() = runTest {
        val store = newStore(clock)
        val template = store.create("Walk", Tier.One, Energy.Low)
        store.assign(template, today.minus(1, DateTimeUnit.DAY))
        assertTrue(store.entriesFor(today).first().isEmpty())
    }

    @Test
    fun `completing awards the tier reward and records a ledger entry`() = runTest {
        val store = newStore(clock)
        val template = store.create("Taxes", Tier.Three, Energy.High)
        val entry = store.assign(template, today)
        assertTrue(store.complete(entry.id))
        assertTrue(store.entriesFor(today).first().single().isCompleted)
        val ledger = store.entries.first().single()
        assertEquals(20, ledger.amount)
        assertEquals(entry.id, ledger.refId)
        assertEquals(clock.current, ledger.timestamp)
    }

    @Test
    fun `an entry completes only once`() = runTest {
        val store = newStore(clock)
        val entry = store.assign(store.create("Taxes", Tier.Three, Energy.High), today)
        assertTrue(store.complete(entry.id))
        assertFalse(store.complete(entry.id))
        assertEquals(20, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `balance sums several completions`() = runTest {
        val store = newStore(clock)
        val small = store.assign(store.create("Small", Tier.One, Energy.Low), today)
        val big = store.assign(store.create("Big", Tier.Four, Energy.High), today)
        store.complete(small.id)
        store.complete(big.id)
        assertEquals(45, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `removing a pending entry awards nothing`() = runTest {
        val store = newStore(clock)
        val entry = store.assign(store.create("Walk", Tier.One, Energy.Low), today)
        store.remove(entry.id)
        assertTrue(store.entriesFor(today).first().isEmpty())
        assertTrue(store.entries.first().isEmpty())
    }

    @Test
    fun `a completed entry cannot be removed`() = runTest {
        val store = newStore(clock)
        val entry = store.assign(store.create("Walk", Tier.One, Energy.Low), today)
        store.complete(entry.id)
        store.remove(entry.id)
        assertEquals(1, store.entriesFor(today).first().size)
    }
}
