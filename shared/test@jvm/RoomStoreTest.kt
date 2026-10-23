package com.usuallycoding.openadhd

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
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
import kotlin.test.assertTrue
import kotlin.time.Clock

/** Runs the shared store contract against a real SQLite database. */
class RoomStoreTest : StoreContractTest() {
    override fun newStore(clock: Clock): OpenAdhdStore {
        val file = createTempDirectory().resolve("contract.db").toString()
        return RoomStore(Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase(), clock)
    }

    @Test
    fun `data survives closing and reopening the database`() = runTest {
        val file = createTempDirectory().resolve("restart.db").toString()
        val today = clock.todayIn(TimeZone.UTC)

        val firstDb = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val first = RoomStore(firstDb, clock)
        val simple = first.create("Taxes", Tier.Three, Energy.High)
        val simpleEntry = first.assign(simple, today)
        assertTrue(first.completeTask(simpleEntry.id))
        val big = first.create("Clean the kitchen", Tier.Three, Energy.Medium, "Whole room", listOf("Dishes", "Counters", "Trash"))
        val bigEntry = first.assign(big, today)
        assertTrue(first.completeSubtask(bigEntry.subtasks[1].id))
        val treat = first.createReward("Movie night", RewardTier.One, Energy.Low, "With snacks")
        assertTrue(first.redeem(treat.id, "r1"))
        val saved = first.createReward("Headphones", RewardTier.Five, Energy.Low, customPrice = 500)
        firstDb.close()

        val second = RoomStore(Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase(), clock)
        assertEquals(listOf(simple, big), second.templates.first())

        val restored = second.entriesFor(today).first().associateBy { it.name }
        assertTrue(restored.getValue("Taxes").isCompleted)
        val restoredBig = restored.getValue("Clean the kitchen")
        assertEquals("Whole room", restoredBig.description)
        assertEquals(listOf("Dishes", "Counters", "Trash"), restoredBig.subtasks.map { it.title })
        assertEquals(listOf(false, true, false), restoredBig.subtasks.map { it.isCompleted })
        assertTrue(restoredBig.hasProgress)
        assertTrue(!restoredBig.isCompleted)
        assertEquals(7, CoinRules.balance(second.entries.first()))
        assertEquals(listOf(treat, saved), second.rewards.first())
        val purchase = second.redemptions.first().single()
        assertEquals("Movie night", purchase.name)
        assertEquals(20, purchase.price)
    }
}
