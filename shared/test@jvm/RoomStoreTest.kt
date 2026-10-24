package com.usuallycoding.openadhd

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.CriticalHits
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.OpenAdhdStore
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.random.Random
import kotlin.time.Clock

/** Runs the shared store contract against a real SQLite database. */
class RoomStoreTest : StoreContractTest() {
    override fun newStore(clock: Clock, random: Random, criticalHits: CriticalHits): OpenAdhdStore {
        val file = createTempDirectory().resolve("contract.db").toString()
        return RoomStore(Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase(), clock, random, criticalHits)
    }

    @Test
    fun `a critical hit is saved with its reason and survives a restart`() = runTest {
        val file = createTempDirectory().resolve("crit.db").toString()
        val today = clock.todayIn(TimeZone.UTC)
        val firstDb = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val first = RoomStore(firstDb, clock, criticalHits = ScriptedCrits(true))
        val template = first.create("Clean", Tier.Three, Energy.Low, subtasks = listOf("A", "B", "C"))
        val entry = first.assign(template, today)
        val result = first.completeSubtask(entry.subtasks.first().id)!!
        assertEquals(7, result.bonus)
        firstDb.close()

        val second = RoomStore(Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase(), clock)
        val ledger = second.entries.first()
        assertEquals(listOf(LedgerReason.SubtaskCompleted, LedgerReason.CriticalHit), ledger.map { it.reason })
        assertEquals(listOf(entry.subtasks.first().id, entry.subtasks.first().id), ledger.map { it.refId })
        assertEquals(14, CoinRules.balance(ledger))
    }

    @Test
    fun `the description of a redemption survives a restart`() = runTest {
        val file = createTempDirectory().resolve("redemption-description.db").toString()
        val today = clock.todayIn(TimeZone.UTC)
        val firstDb = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val first = RoomStore(firstDb, clock)
        first.assign(first.create("Taxes", Tier.Four, Energy.High), today).let { first.completeTask(it.id) }
        val reward = first.createReward("Movie night", RewardTier.One, Energy.Low, "With snacks")
        assertTrue(first.redeem(reward.id, "r1"))
        firstDb.close()

        val second = RoomStore(Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase(), clock)
        assertEquals("With snacks", second.redemptions.first().single().description)
    }

    @Test
    fun `data survives closing and reopening the database`() = runTest {
        val file = createTempDirectory().resolve("restart.db").toString()
        val today = clock.todayIn(TimeZone.UTC)

        val firstDb = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val first = RoomStore(firstDb, clock)
        val simple = first.create("Taxes", Tier.Three, Energy.High)
        val simpleEntry = first.assign(simple, today)
        assertNotNull(first.completeTask(simpleEntry.id))
        val big = first.create("Clean the kitchen", Tier.Three, Energy.Medium, "Whole room", listOf("Dishes", "Counters", "Trash"))
        val bigEntry = first.assign(big, today)
        assertNotNull(first.completeSubtask(bigEntry.subtasks[1].id))
        val treat = first.createReward("Movie night", RewardTier.One, Energy.Low, "With snacks")
        assertTrue(first.redeem(treat.id, "r1"))
        val saved = first.createReward("Headphones", RewardTier.Five, Energy.Low, customPrice = 500)
        val extraTemplate = first.create("Extra", Tier.Four, Energy.High)
        assertNotNull(first.completeTask(first.assign(extraTemplate, today).id))
        val rolled = first.mysteryRoll(RewardTier.One, "roll1")!!
        firstDb.close()

        val second = RoomStore(Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase(), clock)
        assertEquals(listOf(simple, big, extraTemplate), second.templates.first())

        val restored = second.entriesFor(today).first().associateBy { it.name }
        assertTrue(restored.getValue("Taxes").isCompleted)
        val restoredBig = restored.getValue("Clean the kitchen")
        assertEquals("Whole room", restoredBig.description)
        assertEquals(listOf("Dishes", "Counters", "Trash"), restoredBig.subtasks.map { it.title })
        assertEquals(listOf(false, true, false), restoredBig.subtasks.map { it.isCompleted })
        assertTrue(restoredBig.hasProgress)
        assertTrue(!restoredBig.isCompleted)
        assertEquals(27, CoinRules.balance(second.entries.first()))
        assertEquals(listOf(treat, saved), second.rewards.first())
        val history = second.redemptions.first()
        assertEquals(2, history.size)
        val purchase = history.last()
        assertEquals("Movie night", purchase.name)
        assertEquals(20, purchase.price)
        assertEquals(RedemptionSource.Purchase, purchase.source)
        val roll = history.first()
        assertEquals(rolled.redemption, roll)
        assertEquals(RedemptionSource.Roll, roll.source)
        assertEquals(RewardTier.One, roll.rolledTier)
        assertEquals(20, roll.price)
    }
}
