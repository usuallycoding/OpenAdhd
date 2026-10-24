package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.CompletionResult
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class FakeClock(var current: Instant = Instant.parse("2026-10-23T10:00:00Z")) : Clock {
    override fun now(): Instant = current
}

/** Behavior every [OpenAdhdStore] implementation must have. */
abstract class StoreContractTest {
    protected val clock = FakeClock()
    protected abstract fun newStore(
        clock: Clock,
        random: Random = Random.Default,
        criticalHits: CriticalHits = CriticalHits.Never,
    ): OpenAdhdStore

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
        assertNotNull(store.completeTask(entry.id))
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
        assertNotNull(store.completeTask(entry.id))
        assertNull(store.completeTask(entry.id))
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

        assertNotNull(store.completeSubtask(first.id))
        assertNull(store.completeSubtask(first.id))

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
        entry.subtasks.forEach { assertNotNull(store.completeSubtask(it.id)) }
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
        assertNull(store.completeTask(entry.id))
        assertTrue(store.entries.first().isEmpty())
        assertFalse(store.entriesFor(today).first().single().isCompleted)
    }

    @Test
    fun `an unknown subtask cannot be completed`() = runTest {
        val store = newStore(clock)
        assertNull(store.completeSubtask("missing"))
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
        assertNotNull(store.completeTask(entry.id))
        val ledger = store.entries.first().single()
        assertEquals(40, ledger.amount)
        assertEquals(LedgerReason.TaskCompleted, ledger.reason)
    }

    // ---- rewards and the shop

    private suspend fun earn(store: OpenAdhdStore, tier: Tier) {
        val entry = store.assign(store.create("Earn ${tier.level}", tier, Energy.Low), today)
        assertNotNull(store.completeTask(entry.id))
    }

    @Test
    fun `a reward is saved trimmed with its tier price`() = runTest {
        val store = newStore(clock)
        store.createReward("  Movie night ", RewardTier.Two, Energy.Low, description = " With snacks ")
        val reward = store.rewards.first().single()
        assertEquals("Movie night", reward.name)
        assertEquals("With snacks", reward.description)
        assertEquals(RewardTier.Two, reward.tier)
        assertEquals(50, reward.price)
        assertNull(reward.customPrice)
    }

    @Test
    fun `a reward needs a name`() = runTest {
        val store = newStore(clock)
        assertFailsWith<IllegalArgumentException> { store.createReward("   ", RewardTier.One, Energy.Low) }
        assertTrue(store.rewards.first().isEmpty())
    }

    @Test
    fun `tier five needs a custom price above three hundred`() = runTest {
        val store = newStore(clock)
        assertFailsWith<IllegalArgumentException> { store.createReward("Headphones", RewardTier.Five, Energy.Low) }
        assertFailsWith<IllegalArgumentException> {
            store.createReward("Headphones", RewardTier.Five, Energy.Low, customPrice = 300)
        }
        assertTrue(store.rewards.first().isEmpty())

        store.createReward("Cheap", RewardTier.Five, Energy.Low, customPrice = 301)
        store.createReward("Headphones", RewardTier.Five, Energy.Low, customPrice = 500)
        assertEquals(listOf(301, 500), store.rewards.first().map { it.price })
    }

    @Test
    fun `editing a reward follows its new tier and leaving tier five clears the custom price`() = runTest {
        val store = newStore(clock)
        val reward = store.createReward("Treat", RewardTier.Two, Energy.Low)
        store.updateReward(reward.copy(tier = RewardTier.Three))
        assertEquals(120, store.rewards.first().single().price)

        store.updateReward(reward.copy(tier = RewardTier.Five, customPrice = 500))
        assertEquals(500, store.rewards.first().single().price)

        store.updateReward(reward.copy(tier = RewardTier.Two, customPrice = 500))
        val back = store.rewards.first().single()
        assertEquals(50, back.price)
        assertNull(back.customPrice)

        assertFailsWith<IllegalArgumentException> { store.updateReward(reward.copy(tier = RewardTier.Five, customPrice = 300)) }
        assertEquals(RewardTier.Two, store.rewards.first().single().tier)
    }

    @Test
    fun `redeeming spends the price records the purchase and the ledger entry`() = runTest {
        val store = newStore(clock)
        earn(store, Tier.Four)
        earn(store, Tier.Four)
        val reward = store.createReward("Movie night", RewardTier.Two, Energy.Low)

        assertTrue(store.redeem(reward.id, "r1"))

        assertEquals(30, CoinRules.balance(store.entries.first()))
        val spend = store.entries.first().last()
        assertEquals(-50, spend.amount)
        assertEquals(LedgerReason.RewardRedeemed, spend.reason)
        assertEquals("r1", spend.refId)
        val purchase = store.redemptions.first().single()
        assertEquals("r1", purchase.id)
        assertEquals("Movie night", purchase.name)
        assertEquals(50, purchase.price)
        assertEquals(RewardTier.Two, purchase.tier)
        assertEquals(reward.id, purchase.templateId)
        assertEquals(clock.current, purchase.redeemedAt)
    }

    @Test
    fun `redeeming is refused when the balance is short`() = runTest {
        val store = newStore(clock)
        earn(store, Tier.Three)
        val reward = store.createReward("Movie night", RewardTier.Two, Energy.Low)

        assertFalse(store.redeem(reward.id, "r1"))

        assertEquals(20, CoinRules.balance(store.entries.first()))
        assertTrue(store.redemptions.first().isEmpty())
        assertEquals(1, store.entries.first().size)
    }

    @Test
    fun `the whole balance can be spent and no more`() = runTest {
        val store = newStore(clock)
        earn(store, Tier.Four)
        earn(store, Tier.Three)
        earn(store, Tier.One)
        val reward = store.createReward("Dinner out", RewardTier.Three, Energy.High)
        earn(store, Tier.Four)
        assertEquals(105, CoinRules.balance(store.entries.first()))
        assertFalse(store.redeem(reward.id, "short"))

        earn(store, Tier.Two)
        earn(store, Tier.One)
        assertEquals(120, CoinRules.balance(store.entries.first()))
        assertTrue(store.redeem(reward.id, "exact"))
        assertEquals(0, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `an unknown reward cannot be redeemed`() = runTest {
        val store = newStore(clock)
        earn(store, Tier.Four)
        assertFalse(store.redeem("missing", "r1"))
        assertTrue(store.redemptions.first().isEmpty())
        assertEquals(40, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `redeeming twice with the same redemption id records one purchase`() = runTest {
        val store = newStore(clock)
        repeat(3) { earn(store, Tier.Four) }
        val reward = store.createReward("Movie night", RewardTier.Two, Energy.Low)

        assertTrue(store.redeem(reward.id, "same"))
        assertFalse(store.redeem(reward.id, "same"))

        assertEquals(1, store.redemptions.first().size)
        assertEquals(70, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `the same reward can be bought again with a new redemption id`() = runTest {
        val store = newStore(clock)
        repeat(3) { earn(store, Tier.Four) }
        earn(store, Tier.Three)
        earn(store, Tier.One)
        earn(store, Tier.One)
        val reward = store.createReward("Movie night", RewardTier.Two, Energy.Low)
        assertEquals(150, CoinRules.balance(store.entries.first()))

        assertTrue(store.redeem(reward.id, "a"))
        assertTrue(store.redeem(reward.id, "b"))

        assertEquals(50, CoinRules.balance(store.entries.first()))
        assertEquals(2, store.redemptions.first().size)
    }

    @Test
    fun `a redemption keeps what was paid after the reward is edited or deleted`() = runTest {
        val store = newStore(clock)
        repeat(2) { earn(store, Tier.Four) }
        val reward = store.createReward("Movie night", RewardTier.Two, Energy.Low)
        store.redeem(reward.id, "r1")

        store.updateReward(reward.copy(name = "Cinema trip", tier = RewardTier.Three))
        var purchase = store.redemptions.first().single()
        assertEquals("Movie night", purchase.name)
        assertEquals(50, purchase.price)
        assertEquals(RewardTier.Two, purchase.tier)

        store.deleteReward(reward.id)
        assertTrue(store.rewards.first().isEmpty())
        purchase = store.redemptions.first().single()
        assertEquals("Movie night", purchase.name)
        assertEquals(30, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `purchases are listed newest first`() = runTest {
        val store = newStore(clock)
        repeat(3) { earn(store, Tier.Four) }
        val first = store.createReward("First", RewardTier.One, Energy.Low)
        val second = store.createReward("Second", RewardTier.One, Energy.Low)
        store.redeem(first.id, "a")
        store.redeem(second.id, "b")
        assertEquals(listOf("Second", "First"), store.redemptions.first().map { it.name })
    }

    // ---- mystery rolls

    @Test
    fun `a usual roll charges the tier price and records a roll`() = runTest {
        val store = newStore(clock, ScriptedRandom(3, 0))
        earn(store, Tier.Four)
        earn(store, Tier.Three)
        val movie = store.createReward("Movie night", RewardTier.Two, Energy.Low)
        store.createReward("Dinner out", RewardTier.Three, Energy.High)

        val result = store.mysteryRoll(RewardTier.Two, "roll1")!!

        assertEquals(movie.id, result.redemption.templateId)
        assertFalse(result.upgraded)
        assertEquals(10, CoinRules.balance(store.entries.first()))
        val spend = store.entries.first().last()
        assertEquals(-50, spend.amount)
        assertEquals(LedgerReason.MysteryRoll, spend.reason)
        assertEquals("roll1", spend.refId)
        val recorded = store.redemptions.first().single()
        assertEquals(RedemptionSource.Roll, recorded.source)
        assertEquals(RewardTier.Two, recorded.rolledTier)
        assertEquals(RewardTier.Two, recorded.tier)
        assertEquals(50, recorded.price)
        assertFalse(recorded.isLuckyUpgrade)
    }

    @Test
    fun `a lucky upgrade gives a higher tier reward for the rolled tier price`() = runTest {
        val store = newStore(clock, ScriptedRandom(0, 0))
        earn(store, Tier.Four)
        earn(store, Tier.Three)
        store.createReward("Movie night", RewardTier.Two, Energy.Low)
        store.createReward("Dinner out", RewardTier.Three, Energy.High)

        val result = store.mysteryRoll(RewardTier.Two, "roll1")!!

        assertTrue(result.upgraded)
        assertEquals("Dinner out", result.redemption.name)
        assertEquals(RewardTier.Three, result.redemption.tier)
        assertEquals(RewardTier.Two, result.redemption.rolledTier)
        assertEquals(50, result.redemption.price)
        assertEquals(10, CoinRules.balance(store.entries.first()))
        assertTrue(store.redemptions.first().single().isLuckyUpgrade)
    }

    @Test
    fun `an upgrade without a higher tier gives a reward from the rolled tier`() = runTest {
        val store = newStore(clock, ScriptedRandom(0, 0))
        earn(store, Tier.Four)
        earn(store, Tier.Three)
        store.createReward("Movie night", RewardTier.Two, Energy.Low)

        val result = store.mysteryRoll(RewardTier.Two, "roll1")!!

        assertFalse(result.upgraded)
        assertEquals("Movie night", result.redemption.name)
        assertEquals(RewardTier.Two, result.redemption.tier)
    }

    @Test
    fun `a roll is refused when the balance is short`() = runTest {
        val store = newStore(clock, ScriptedRandom())
        earn(store, Tier.Three)
        store.createReward("Movie night", RewardTier.Two, Energy.Low)

        assertNull(store.mysteryRoll(RewardTier.Two, "roll1"))

        assertEquals(20, CoinRules.balance(store.entries.first()))
        assertTrue(store.redemptions.first().isEmpty())
        assertEquals(1, store.entries.first().size)
    }

    @Test
    fun `a roll of an empty tier is refused`() = runTest {
        val store = newStore(clock, ScriptedRandom())
        earn(store, Tier.Four)
        earn(store, Tier.Three)
        store.createReward("Dinner out", RewardTier.Three, Energy.High)

        assertNull(store.mysteryRoll(RewardTier.Two, "roll1"))

        assertEquals(60, CoinRules.balance(store.entries.first()))
        assertTrue(store.redemptions.first().isEmpty())
    }

    @Test
    fun `rolling twice with the same id rolls once`() = runTest {
        val store = newStore(clock, ScriptedRandom(3, 0))
        repeat(2) { earn(store, Tier.Four) }
        store.createReward("Movie night", RewardTier.Two, Energy.Low)

        assertNotNull(store.mysteryRoll(RewardTier.Two, "same"))
        assertNull(store.mysteryRoll(RewardTier.Two, "same"))

        assertEquals(1, store.redemptions.first().size)
        assertEquals(30, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `tier four never upgrades and tier five is never rolled or won`() = runTest {
        val store = newStore(clock, ScriptedRandom(0, 0))
        repeat(8) { earn(store, Tier.Four) }
        store.createReward("Weekend away", RewardTier.Four, Energy.High)
        store.createReward("Headphones", RewardTier.Five, Energy.Low, customPrice = 500)

        val result = store.mysteryRoll(RewardTier.Four, "roll1")!!

        assertFalse(result.upgraded)
        assertEquals("Weekend away", result.redemption.name)
        assertEquals(20, CoinRules.balance(store.entries.first()))
        assertNull(store.mysteryRoll(RewardTier.Five, "roll2"))
        assertEquals(1, store.redemptions.first().size)
    }

    @Test
    fun `a purchase is still recorded as a purchase`() = runTest {
        val store = newStore(clock, ScriptedRandom(3, 0))
        repeat(3) { earn(store, Tier.Four) }
        val reward = store.createReward("Movie night", RewardTier.Two, Energy.Low)
        assertTrue(store.redeem(reward.id, "buy"))
        store.mysteryRoll(RewardTier.Two, "roll")

        val newestFirst = store.redemptions.first()
        assertEquals(listOf(RedemptionSource.Roll, RedemptionSource.Purchase), newestFirst.map { it.source })
        assertNull(newestFirst.last().rolledTier)
        assertFalse(newestFirst.last().isLuckyUpgrade)
        assertEquals(20, CoinRules.balance(store.entries.first()))
    }

    // ---- critical hits

    @Test
    fun `a usual completion pays only the reward`() = runTest {
        val store = newStore(clock, criticalHits = ScriptedCrits(false))
        val entry = store.assign(store.create("Taxes", Tier.Three, Energy.High), today)

        val result = store.completeTask(entry.id)!!

        assertEquals(20, result.reward)
        assertEquals(0, result.bonus)
        assertFalse(result.isCritical)
        assertEquals(1, store.entries.first().size)
        assertEquals(20, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `a critical task pays the reward and an equal bonus as two entries`() = runTest {
        val store = newStore(clock, criticalHits = ScriptedCrits(true))
        val entry = store.assign(store.create("Taxes", Tier.Three, Energy.High), today)

        val result = store.completeTask(entry.id)!!

        assertTrue(result.isCritical)
        assertEquals(20, result.reward)
        assertEquals(20, result.bonus)
        val ledger = store.entries.first()
        assertEquals(listOf(20, 20), ledger.map { it.amount })
        assertEquals(listOf(LedgerReason.TaskCompleted, LedgerReason.CriticalHit), ledger.map { it.reason })
        assertEquals(listOf(entry.id, entry.id), ledger.map { it.refId })
        assertEquals(40, CoinRules.balance(ledger))
    }

    @Test
    fun `a critical subtask pays its share twice with two entries`() = runTest {
        val store = newStore(clock, criticalHits = ScriptedCrits(true))
        val entry = store.assign(store.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B", "C")), today)
        val first = entry.subtasks.first()

        val result = store.completeSubtask(first.id)!!

        assertEquals(CompletionResult(reward = 7, bonus = 7), result)
        val ledger = store.entries.first()
        assertEquals(listOf(LedgerReason.SubtaskCompleted, LedgerReason.CriticalHit), ledger.map { it.reason })
        assertEquals(listOf(first.id, first.id), ledger.map { it.refId })
        assertEquals(14, CoinRules.balance(ledger))
    }

    @Test
    fun `each subtask is decided on its own`() = runTest {
        val store = newStore(clock, criticalHits = ScriptedCrits(true, false, true))
        val entry = store.assign(store.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B", "C")), today)

        val results = entry.subtasks.map { store.completeSubtask(it.id)!! }

        assertEquals(listOf(true, false, true), results.map { it.isCritical })
        assertEquals(listOf(7, 0, 6), results.map { it.bonus })
        // 20 in rewards plus 13 in bonuses
        assertEquals(33, CoinRules.balance(store.entries.first()))
        assertEquals(5, store.entries.first().size)
    }

    @Test
    fun `a repeated completion neither pays nor rolls again`() = runTest {
        val crits = ScriptedCrits(false)
        val store = newStore(clock, criticalHits = crits)
        val entry = store.assign(store.create("Taxes", Tier.Three, Energy.High), today)
        assertNotNull(store.completeTask(entry.id))
        assertEquals(0, crits.remaining)

        assertNull(store.completeTask(entry.id))

        assertEquals(20, CoinRules.balance(store.entries.first()))
    }

    @Test
    fun `refused completions never roll`() = runTest {
        val crits = ScriptedCrits()
        val store = newStore(clock, criticalHits = crits)
        val withSubtasks = store.assign(store.create("Clean", Tier.Two, Energy.Low, subtasks = listOf("A")), today)

        assertNull(store.completeTask(withSubtasks.id))
        assertNull(store.completeTask("missing"))
        assertNull(store.completeSubtask("missing"))

        assertTrue(store.entries.first().isEmpty())
    }

    @Test
    fun `a repeated subtask completion neither pays nor rolls again`() = runTest {
        val crits = ScriptedCrits(true)
        val store = newStore(clock, criticalHits = crits)
        val entry = store.assign(store.create("Clean", Tier.Two, Energy.Low, subtasks = listOf("A", "B")), today)
        val sub = entry.subtasks.first()
        assertNotNull(store.completeSubtask(sub.id))

        assertNull(store.completeSubtask(sub.id))

        assertEquals(10, CoinRules.balance(store.entries.first()))
        assertEquals(2, store.entries.first().size)
    }

    @Test
    fun `the bonus follows the frozen reward even after the template changes`() = runTest {
        val store = newStore(clock, criticalHits = ScriptedCrits(true))
        val template = store.create("Taxes", Tier.Three, Energy.High)
        val entry = store.assign(template, today)
        store.update(template.copy(tier = Tier.One))

        val result = store.completeTask(entry.id)!!

        assertEquals(CompletionResult(reward = 20, bonus = 20), result)
    }

    @Test
    fun `stores have no critical hits unless asked`() = runTest {
        val store = newStore(clock)
        repeat(40) {
            val entry = store.assign(store.create("Task $it", Tier.One, Energy.Low), today)
            assertFalse(store.completeTask(entry.id)!!.isCritical)
        }
        assertEquals(200, CoinRules.balance(store.entries.first()))
    }

    // ---- ordering

    private suspend fun OpenAdhdStore.templateNames() = templates.first().map { it.name }
    private suspend fun OpenAdhdStore.entryNames(date: LocalDate) = entriesFor(date).first().map { it.name }
    private suspend fun OpenAdhdStore.rewardNames() = rewards.first().map { it.name }

    @Test
    fun `templates follow the order they are reordered into and new ones go last`() = runTest {
        val store = newStore(clock)
        val a = store.create("A", Tier.One, Energy.Low)
        val b = store.create("B", Tier.One, Energy.Low)
        val c = store.create("C", Tier.One, Energy.Low)
        store.reorderTemplates(listOf(c.id, a.id, b.id))
        assertEquals(listOf("C", "A", "B"), store.templateNames())
        store.create("D", Tier.One, Energy.Low)
        assertEquals(listOf("C", "A", "B", "D"), store.templateNames())
    }

    @Test
    fun `reordering ignores unknown ids and keeps unnamed items after the named ones`() = runTest {
        val store = newStore(clock)
        val a = store.create("A", Tier.One, Energy.Low)
        store.create("B", Tier.One, Energy.Low)
        val c = store.create("C", Tier.One, Energy.Low)
        store.reorderTemplates(listOf("nope", c.id, "nope", a.id))
        assertEquals(listOf("C", "A", "B"), store.templateNames())
        store.reorderTemplates(emptyList())
        assertEquals(listOf("C", "A", "B"), store.templateNames())
    }

    @Test
    fun `editing and deleting do not disturb the order`() = runTest {
        val store = newStore(clock)
        val a = store.create("A", Tier.One, Energy.Low)
        val b = store.create("B", Tier.One, Energy.Low)
        val c = store.create("C", Tier.One, Energy.Low)
        store.reorderTemplates(listOf(c.id, a.id, b.id))
        store.update(a.copy(name = "A2"))
        assertEquals(listOf("C", "A2", "B"), store.templateNames())
        store.delete(c.id)
        assertEquals(listOf("A2", "B"), store.templateNames())
        store.create("D", Tier.One, Energy.Low)
        assertEquals(listOf("A2", "B", "D"), store.templateNames())
    }

    @Test
    fun `rewards follow the order they are reordered into and new ones go last`() = runTest {
        val store = newStore(clock)
        val a = store.createReward("A", RewardTier.One, Energy.Low)
        val b = store.createReward("B", RewardTier.Two, Energy.Low)
        store.reorderRewards(listOf(b.id, a.id))
        assertEquals(listOf("B", "A"), store.rewardNames())
        store.createReward("C", RewardTier.One, Energy.Low)
        assertEquals(listOf("B", "A", "C"), store.rewardNames())
    }

    @Test
    fun `entries are reordered per day and new entries go last`() = runTest {
        val store = newStore(clock)
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val a = store.create("A", Tier.One, Energy.Low)
        val b = store.create("B", Tier.One, Energy.Low)
        val c = store.create("C", Tier.One, Energy.Low)
        val e1 = store.assign(a, today)
        store.assign(a, yesterday)
        val e2 = store.assign(b, today)
        store.assign(b, yesterday)
        val e3 = store.assign(c, today)
        store.reorderEntries(today, listOf(e3.id, e1.id, e2.id))
        assertEquals(listOf("C", "A", "B"), store.entryNames(today))
        assertEquals(listOf("A", "B"), store.entryNames(yesterday))
        store.assign(a, today)
        assertEquals(listOf("C", "A", "B", "A"), store.entryNames(today))
    }

    @Test
    fun `completing an entry keeps its place in the stored order`() = runTest {
        val store = newStore(clock)
        val a = store.assign(store.create("A", Tier.One, Energy.Low), today)
        val b = store.assign(store.create("B", Tier.One, Energy.Low), today)
        store.reorderEntries(today, listOf(b.id, a.id))
        store.completeTask(b.id)
        assertEquals(listOf("B", "A"), store.entryNames(today))
    }
}
