package com.usuallycoding.openadhd

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoomMigrationTest {
    private val day = LocalDate(2026, 10, 23)

    /** Builds a database exactly as version 1 of the app left it, using raw SQL. */
    private fun createVersion1Database(): String {
        val file = createTempDirectory().resolve("v1.db").toString()
        val connection = BundledSQLiteDriver().open(file)
        try {
            connection.execSQL("CREATE TABLE templates (id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_tasks (id TEXT NOT NULL, date TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, templateId TEXT, completedAtMillis INTEGER, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE ledger (id TEXT NOT NULL, amount INTEGER NOT NULL, timestampMillis INTEGER NOT NULL, reason TEXT NOT NULL, refId TEXT, PRIMARY KEY(id))")
            connection.execSQL("INSERT INTO templates VALUES ('t1', 'Reply to emails', 2, 'Medium')")
            connection.execSQL("INSERT INTO planned_tasks VALUES ('p1', '$day', 'Taxes', 3, 'High', NULL, 1000)")
            connection.execSQL("INSERT INTO planned_tasks VALUES ('p2', '$day', 'Gym', 4, 'High', 't1', NULL)")
            connection.execSQL("INSERT INTO ledger VALUES ('l1', 20, 1000, 'TaskCompleted', 'p1')")
            connection.execSQL("PRAGMA user_version = 1")
        } finally {
            connection.close()
        }
        return file
    }

    @Test
    fun `upgrading from version 1 keeps templates entries and the balance`() = runTest {
        val file = createVersion1Database()
        val database = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        val template = store.templates.first().single()
        assertEquals("Reply to emails", template.name)
        assertEquals(Tier.Two, template.tier)
        assertEquals("", template.description)
        assertTrue(template.subtasks.isEmpty())

        val entries = store.entriesFor(day).first().associateBy { it.name }
        val taxes = entries.getValue("Taxes")
        assertTrue(taxes.isCompleted)
        assertEquals(20, taxes.reward)
        assertTrue(taxes.subtasks.isEmpty())
        val gym = entries.getValue("Gym")
        assertTrue(!gym.isCompleted)
        assertEquals(40, gym.reward)

        assertEquals(20, CoinRules.balance(store.entries.first()))
        database.close()
    }

    @Test
    fun `an upgraded database pays old entries their backfilled reward and accepts new features`() = runTest {
        val file = createVersion1Database()
        val database = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        val gym = store.entriesFor(day).first().single { it.name == "Gym" }
        assertTrue(store.completeTask(gym.id))
        assertEquals(60, CoinRules.balance(store.entries.first()))

        val template = store.create("Clean", Tier.Three, Energy.Low, "Whole room", listOf("A", "B", "C"))
        val entry = store.assign(template, day)
        store.completeSubtask(entry.subtasks.first().id)
        assertEquals(67, CoinRules.balance(store.entries.first()))
        database.close()
    }

    /** Builds a database exactly as version 2 of the app left it, using raw SQL. */
    private fun createVersion2Database(): String {
        val file = createTempDirectory().resolve("v2.db").toString()
        val connection = BundledSQLiteDriver().open(file)
        try {
            connection.execSQL("CREATE TABLE templates (id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE template_subtasks (id TEXT NOT NULL, templateId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_tasks (id TEXT NOT NULL, date TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, templateId TEXT, completedAtMillis INTEGER, description TEXT NOT NULL DEFAULT '', reward INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_subtasks (id TEXT NOT NULL, plannedTaskId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, reward INTEGER NOT NULL, completedAtMillis INTEGER, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE ledger (id TEXT NOT NULL, amount INTEGER NOT NULL, timestampMillis INTEGER NOT NULL, reason TEXT NOT NULL, refId TEXT, PRIMARY KEY(id))")
            connection.execSQL("INSERT INTO templates VALUES ('t1', 'Clean', 3, 'Low', 'Whole room')")
            connection.execSQL("INSERT INTO template_subtasks VALUES ('t1#0', 't1', 0, 'Dishes')")
            connection.execSQL("INSERT INTO planned_tasks VALUES ('p1', '$day', 'Clean', 3, 'Low', 't1', NULL, 'Whole room', 20)")
            connection.execSQL("INSERT INTO planned_subtasks VALUES ('s1', 'p1', 0, 'Dishes', 20, 1000)")
            connection.execSQL("INSERT INTO ledger VALUES ('l1', 20, 1000, 'SubtaskCompleted', 's1')")
            connection.execSQL("INSERT INTO ledger VALUES ('l2', 40, 2000, 'TaskCompleted', 'p0')")
            connection.execSQL("PRAGMA user_version = 2")
        } finally {
            connection.close()
        }
        return file
    }

    @Test
    fun `upgrading from version 2 keeps everything and adds an empty usable shop`() = runTest {
        val file = createVersion2Database()
        val database = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        val template = store.templates.first().single()
        assertEquals("Whole room", template.description)
        assertEquals(listOf("Dishes"), template.subtasks)
        val entry = store.entriesFor(day).first().single()
        assertEquals(listOf(true), entry.subtasks.map { it.isCompleted })
        assertEquals(60, CoinRules.balance(store.entries.first()))
        assertTrue(store.rewards.first().isEmpty())
        assertTrue(store.redemptions.first().isEmpty())

        val reward = store.createReward("Movie night", RewardTier.Two, Energy.Low)
        assertTrue(store.redeem(reward.id, "r1"))
        assertEquals(10, CoinRules.balance(store.entries.first()))
        assertEquals(listOf("Movie night"), store.redemptions.first().map { it.name })
        database.close()
    }

    @Test
    fun `upgrading from version 1 through both migrations keeps the balance and opens the shop`() = runTest {
        val file = createVersion1Database()
        val database = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        assertEquals(20, CoinRules.balance(store.entries.first()))
        assertTrue(store.rewards.first().isEmpty())

        val reward = store.createReward("Treat", RewardTier.One, Energy.Low)
        assertTrue(store.redeem(reward.id, "r1"))
        assertEquals(0, CoinRules.balance(store.entries.first()))
        database.close()
    }
}
