package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerReason
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
        store.completeTask(entry.id)
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
        assertTrue(store.completeTask(entry.id))
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
        assertTrue(store.completeTask(entry.id))
        assertFalse(store.completeTask(entry.id))
        assertEquals(20, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `balance sums several completions`() = runTest {
        val store = newStore(clock)
        val small = store.assign(store.create("Small", Tier.One, Energy.Low), today)
        val big = store.assign(store.create("Big", Tier.Four, Energy.High), today)
        store.completeTask(small.id)
        store.completeTask(big.id)
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
        store.completeTask(entry.id)
        store.remove(entry.id)
        assertEquals(1, store.entriesFor(today).first().size)
    }

    // ---- descriptions and subtasks

    @Test
    fun `description and subtasks are saved trimmed and in order`() = runTest {
        val store = newStore(clock)
        store.create(
            "Clean the kitchen", Tier.Three, Energy.Medium,
            description = "  The whole room  ",
            subtasks = listOf(" Dishes ", "Wipe counters", "Take out trash"),
        )
        val template = store.templates.first().single()
        assertEquals("The whole room", template.description)
        assertEquals(listOf("Dishes", "Wipe counters", "Take out trash"), template.subtasks)
    }

    @Test
    fun `a blank subtask title is rejected`() = runTest {
        val store = newStore(clock)
        assertFailsWith<IllegalArgumentException> {
            store.create("Clean", Tier.One, Energy.Low, subtasks = listOf("Dishes", "   "))
        }
        assertTrue(store.templates.first().isEmpty())
        val template = store.create("Clean", Tier.One, Energy.Low)
        assertFailsWith<IllegalArgumentException> { store.update(template.copy(subtasks = listOf(""))) }
    }

    @Test
    fun `the subtask limit follows the tier`() = runTest {
        val store = newStore(clock)
        store.create("Small", Tier.One, Energy.Low, subtasks = List(5) { "Step $it" })
        store.create("Large", Tier.Four, Energy.Low, subtasks = List(10) { "Step $it" })
        assertEquals(listOf(5, 10), store.templates.first().map { it.subtasks.size })
        assertFailsWith<IllegalArgumentException> {
            store.create("Too many small", Tier.One, Energy.Low, subtasks = List(6) { "Step $it" })
        }
        assertFailsWith<IllegalArgumentException> {
            store.create("Too many large", Tier.Four, Energy.Low, subtasks = List(11) { "Step $it" })
        }
        assertEquals(2, store.templates.first().size)
    }

    @Test
    fun `lowering the tier below the subtask count is rejected`() = runTest {
        val store = newStore(clock)
        val template = store.create("Clean", Tier.Two, Energy.Low, subtasks = List(8) { "Step $it" })
        assertFailsWith<IllegalArgumentException> { store.update(template.copy(tier = Tier.One)) }
        assertEquals(Tier.Two, store.templates.first().single().tier)
        store.update(template.copy(tier = Tier.Three))
        assertEquals(Tier.Three, store.templates.first().single().tier)
    }

    @Test
    fun `an entry copies the description and subtasks with frozen coin shares`() = runTest {
        val store = newStore(clock)
        val template = store.create("Clean", Tier.Three, Energy.Low, "Whole room", listOf("A", "B", "C"))
        val entry = store.assign(template, today)
        assertEquals("Whole room", entry.description)
        assertEquals(20, entry.reward)
        assertEquals(listOf("A", "B", "C"), entry.subtasks.map { it.title })
        assertEquals(listOf(7, 7, 6), entry.subtasks.map { it.reward })
        assertTrue(entry.subtasks.none { it.isCompleted })
        assertFalse(entry.isCompleted)
    }

    @Test
    fun `editing a template after assignment leaves the entry and its shares unchanged`() = runTest {
        val store = newStore(clock)
        val template = store.create("Clean", Tier.Three, Energy.Low, "Old", listOf("A", "B", "C"))
        store.assign(template, today)
        store.update(template.copy(tier = Tier.One, description = "New", subtasks = listOf("Only")))
        val entry = store.entriesFor(today).first().single()
        assertEquals("Old", entry.description)
        assertEquals(listOf(7, 7, 6), entry.subtasks.map { it.reward })
    }

    @Test
    fun `completing a subtask pays its share once and records it`() = runTest {
        val store = newStore(clock)
        val entry = store.assign(store.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B", "C")), today)
        val first = entry.subtasks.first()

        assertTrue(store.completeSubtask(first.id))
        assertFalse(store.completeSubtask(first.id))

        val ledger = store.entries.first().single()
        assertEquals(7, ledger.amount)
        assertEquals(LedgerReason.SubtaskCompleted, ledger.reason)
        assertEquals(first.id, ledger.refId)
        val stored = store.entriesFor(today).first().single()
        assertTrue(stored.subtasks.first().isCompleted)
        assertFalse(stored.isCompleted)
        assertTrue(stored.hasProgress)
    }

    @Test
    fun `finishing the last subtask completes the entry and pays exactly the pool`() = runTest {
        val store = newStore(clock)
        val entry = store.assign(store.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B", "C")), today)
        entry.subtasks.forEach { assertTrue(store.completeSubtask(it.id)) }
        assertTrue(store.entriesFor(today).first().single().isCompleted)
        assertEquals(20, CoinRules.balance(store.entries.first()))
        assertEquals(3, store.entries.first().size)
    }

    @Test
    fun `a pool split into as many subtasks as coins pays one coin each`() = runTest {
        val store = newStore(clock)
        val entry = store.assign(store.create("Tiny", Tier.One, Energy.Low, subtasks = List(5) { "Step $it" }), today)
        assertTrue(entry.subtasks.all { it.reward == 1 })
        entry.subtasks.forEach { store.completeSubtask(it.id) }
        assertEquals(5, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `an entry with subtasks cannot be completed directly`() = runTest {
        val store = newStore(clock)
        val entry = store.assign(store.create("Clean", Tier.Two, Energy.Low, subtasks = listOf("A")), today)
        assertFalse(store.completeTask(entry.id))
        assertTrue(store.entries.first().isEmpty())
        assertFalse(store.entriesFor(today).first().single().isCompleted)
    }

    @Test
    fun `an unknown subtask cannot be completed`() = runTest {
        val store = newStore(clock)
        assertFalse(store.completeSubtask("missing"))
        assertTrue(store.entries.first().isEmpty())
    }

    @Test
    fun `an entry in progress cannot be removed but an untouched one can`() = runTest {
        val store = newStore(clock)
        val template = store.create("Clean", Tier.Two, Energy.Low, subtasks = listOf("A", "B"))
        val started = store.assign(template, today)
        val untouched = store.assign(template, today)
        store.completeSubtask(started.subtasks.first().id)

        store.remove(started.id)
        store.remove(untouched.id)

        assertEquals(listOf(started.id), store.entriesFor(today).first().map { it.id })
    }

    @Test
    fun `an entry without subtasks still pays its whole pool`() = runTest {
        val store = newStore(clock)
        val entry = store.assign(store.create("Taxes", Tier.Four, Energy.High), today)
        assertEquals(40, entry.reward)
        assertTrue(store.completeTask(entry.id))
        val ledger = store.entries.first().single()
        assertEquals(40, ledger.amount)
        assertEquals(LedgerReason.TaskCompleted, ledger.reason)
    }
}
