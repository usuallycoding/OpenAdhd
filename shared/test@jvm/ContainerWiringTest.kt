package com.usuallycoding.openadhd

import androidx.room.Room
import com.usuallycoding.openadhd.data.createContainer
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.LedgerReason
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** The app's real wiring (used by Android, iOS and desktop) must have critical hits switched on. */
class ContainerWiringTest {
    @Test
    fun `the shared container produces critical hits at about one in eight`() = runTest {
        val file = createTempDirectory().resolve("wiring.db").toString()
        val database = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val store = createContainer(database).store
        val template = store.create("Tiny", Tier.One, Energy.Low)
        val day = LocalDate(2026, 10, 24)

        val completions = 800
        repeat(completions) { assertNotNull(store.completeTask(store.assign(template, day).id)) }

        val ledger = store.entries.first()
        val crits = ledger.count { it.reason == LedgerReason.CriticalHit }
        // 800 completions at 1 in 8 is 100 on average, with a standard deviation of about 9
        assertTrue(crits in 50..160, "expected about 100 critical hits, got $crits")
        assertEquals(completions * 5 + crits * 5, CoinRules.balance(ledger))
        database.close()
    }
}
