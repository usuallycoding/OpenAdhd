package com.usuallycoding.openadhd

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
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
        val template = first.create("Taxes", Tier.Three, Energy.High)
        val entry = first.assign(template, today)
        assertTrue(first.complete(entry.id))
        firstDb.close()

        val second = RoomStore(Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase(), clock)
        assertEquals(listOf(template), second.templates.first())
        val restored = second.entriesFor(today).first().single()
        assertTrue(restored.isCompleted)
        assertEquals("Taxes", restored.name)
        assertEquals(20, CoinRules.balance(second.entries.first()))
    }
}
