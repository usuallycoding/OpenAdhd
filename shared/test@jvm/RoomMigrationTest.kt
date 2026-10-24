package com.usuallycoding.openadhd

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.CoinRules
import com.usuallycoding.openadhd.domain.Energy
import com.usuallycoding.openadhd.domain.RedemptionSource
import com.usuallycoding.openadhd.domain.RewardTier
import com.usuallycoding.openadhd.domain.SettingKey
import com.usuallycoding.openadhd.domain.Settings
import com.usuallycoding.openadhd.domain.Tier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        assertNotNull(store.completeTask(gym.id))
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

    /** Builds a database exactly as version 3 of the app left it (shop, no rolls), using raw SQL. */
    private fun createVersion3Database(): String {
        val file = createTempDirectory().resolve("v3.db").toString()
        val connection = BundledSQLiteDriver().open(file)
        try {
            connection.execSQL("CREATE TABLE templates (id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE template_subtasks (id TEXT NOT NULL, templateId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_tasks (id TEXT NOT NULL, date TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, templateId TEXT, completedAtMillis INTEGER, description TEXT NOT NULL DEFAULT '', reward INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_subtasks (id TEXT NOT NULL, plannedTaskId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, reward INTEGER NOT NULL, completedAtMillis INTEGER, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE ledger (id TEXT NOT NULL, amount INTEGER NOT NULL, timestampMillis INTEGER NOT NULL, reason TEXT NOT NULL, refId TEXT, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE reward_templates (id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, description TEXT NOT NULL, customPrice INTEGER, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE redemptions (id TEXT NOT NULL, templateId TEXT, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, price INTEGER NOT NULL, redeemedAtMillis INTEGER NOT NULL, PRIMARY KEY(id))")
            connection.execSQL("INSERT INTO reward_templates VALUES ('w1', 'Movie night', 2, 'Low', '', NULL)")
            connection.execSQL("INSERT INTO redemptions VALUES ('old1', 'w1', 'Movie night', 2, 'Low', 50, 3000)")
            connection.execSQL("INSERT INTO ledger VALUES ('l1', 100, 1000, 'TaskCompleted', 'p0')")
            connection.execSQL("INSERT INTO ledger VALUES ('l2', 50, 2000, 'TaskCompleted', 'p1')")
            connection.execSQL("INSERT INTO ledger VALUES ('l3', -50, 3000, 'RewardRedeemed', 'old1')")
            connection.execSQL("PRAGMA user_version = 3")
        } finally {
            connection.close()
        }
        return file
    }

    @Test
    fun `upgrading from version 3 keeps earlier purchases as purchases and allows rolls`() = runTest {
        val file = createVersion3Database()
        val database = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        val earlier = store.redemptions.first().single()
        assertEquals("Movie night", earlier.name)
        assertEquals(RedemptionSource.Purchase, earlier.source)
        assertNull(earlier.rolledTier)
        assertEquals(100, CoinRules.balance(store.entries.first()))

        val result = store.mysteryRoll(RewardTier.Two, "roll1")!!
        assertEquals(50, CoinRules.balance(store.entries.first()))
        assertEquals(RedemptionSource.Roll, result.redemption.source)
        assertEquals(listOf(RedemptionSource.Roll, RedemptionSource.Purchase), store.redemptions.first().map { it.source })
        database.close()
    }

    @Test
    fun `upgrading from version 1 through every migration opens rolls`() = runTest {
        val file = createVersion1Database()
        val database = Room.databaseBuilder<OpenAdhdDatabase>(file).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        assertEquals(20, CoinRules.balance(store.entries.first()))
        store.createReward("Treat", RewardTier.One, Energy.Low)
        val result = store.mysteryRoll(RewardTier.One, "roll1")!!
        assertEquals("Treat", result.redemption.name)
        assertEquals(0, CoinRules.balance(store.entries.first()))
        database.close()
    }

    /** A version 4 database with three templates, two rewards and Today entries on two days, in insertion order. */
    private fun createVersion4Database(): String {
        val file = createTempDirectory().resolve("v4.db").toString()
        val connection = BundledSQLiteDriver().open(file)
        try {
            connection.execSQL("CREATE TABLE templates (id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE template_subtasks (id TEXT NOT NULL, templateId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_tasks (id TEXT NOT NULL, date TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, templateId TEXT, completedAtMillis INTEGER, description TEXT NOT NULL DEFAULT '', reward INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_subtasks (id TEXT NOT NULL, plannedTaskId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, reward INTEGER NOT NULL, completedAtMillis INTEGER, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE ledger (id TEXT NOT NULL, amount INTEGER NOT NULL, timestampMillis INTEGER NOT NULL, reason TEXT NOT NULL, refId TEXT, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE reward_templates (id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, description TEXT NOT NULL, customPrice INTEGER, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE redemptions (id TEXT NOT NULL, templateId TEXT, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, price INTEGER NOT NULL, redeemedAtMillis INTEGER NOT NULL, source TEXT NOT NULL DEFAULT 'Purchase', rolledTier INTEGER, PRIMARY KEY(id))")
            // ids are deliberately not in alphabetical order, so only the stored order can explain the result
            connection.execSQL("INSERT INTO templates (id, name, tier, energy) VALUES ('z', 'Zeta', 1, 'Low'), ('m', 'Mu', 2, 'Medium'), ('a', 'Alpha', 3, 'High')")
            connection.execSQL("INSERT INTO reward_templates VALUES ('r2', 'Movie', 2, 'Low', '', NULL), ('r1', 'Tea', 1, 'Low', '', NULL)")
            connection.execSQL("INSERT INTO planned_tasks (id, date, name, tier, energy, reward) VALUES ('p3', '$day', 'Third', 1, 'Low', 5), ('p1', '2026-10-22', 'Old', 1, 'Low', 5), ('p2', '$day', 'Second', 1, 'Low', 5)")
            connection.execSQL("PRAGMA user_version = 4")
        } finally {
            connection.close()
        }
        return file
    }

    @Test
    fun `upgrading from version 4 keeps every list in its order and puts new templates first`() = runTest {
        val database = Room.databaseBuilder<OpenAdhdDatabase>(createVersion4Database()).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        assertEquals(listOf("Zeta", "Mu", "Alpha"), store.templates.first().map { it.name })
        assertEquals(listOf("Movie", "Tea"), store.rewards.first().map { it.name })
        assertEquals(listOf("Third", "Second"), store.entriesFor(day).first().map { it.name })
        assertEquals(listOf("Old"), store.entriesFor(LocalDate(2026, 10, 22)).first().map { it.name })

        store.create("New", Tier.One, Energy.Low)
        store.createReward("Walk", RewardTier.One, Energy.Low)
        store.assign(store.templates.first().first(), day)
        assertEquals(listOf("New", "Zeta", "Mu", "Alpha"), store.templates.first().map { it.name })
        assertEquals(listOf("Walk", "Movie", "Tea"), store.rewards.first().map { it.name })
        // a task added to today still goes after the entries already there
        assertEquals(listOf("Third", "Second", "New"), store.entriesFor(day).first().map { it.name })

        store.reorderTemplates(listOf("a"))
        assertEquals(listOf("Alpha", "New", "Zeta", "Mu"), store.templates.first().map { it.name })
        database.close()
    }

    @Test
    fun `editing a template or reward after the upgrade keeps its place`() = runTest {
        val database = Room.databaseBuilder<OpenAdhdDatabase>(createVersion4Database()).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        store.update(store.templates.first().first { it.id == "m" }.copy(name = "Mu2"))
        store.updateReward(store.rewards.first().first { it.id == "r2" }.copy(name = "Cinema"))
        assertEquals(listOf("Zeta", "Mu2", "Alpha"), store.templates.first().map { it.name })
        assertEquals(listOf("Cinema", "Tea"), store.rewards.first().map { it.name })
        database.close()
    }

    /** A version 5 database with redemptions of a reward that exists, one that was deleted, and one without a template id. */
    private fun createVersion5Database(): String {
        val file = createTempDirectory().resolve("v5.db").toString()
        val connection = BundledSQLiteDriver().open(file)
        try {
            connection.execSQL("CREATE TABLE templates (id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, description TEXT NOT NULL DEFAULT '', position INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE template_subtasks (id TEXT NOT NULL, templateId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_tasks (id TEXT NOT NULL, date TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, templateId TEXT, completedAtMillis INTEGER, description TEXT NOT NULL DEFAULT '', reward INTEGER NOT NULL DEFAULT 0, position INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE planned_subtasks (id TEXT NOT NULL, plannedTaskId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, reward INTEGER NOT NULL, completedAtMillis INTEGER, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE ledger (id TEXT NOT NULL, amount INTEGER NOT NULL, timestampMillis INTEGER NOT NULL, reason TEXT NOT NULL, refId TEXT, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE reward_templates (id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, description TEXT NOT NULL, customPrice INTEGER, position INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(id))")
            connection.execSQL("CREATE TABLE redemptions (id TEXT NOT NULL, templateId TEXT, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, price INTEGER NOT NULL, redeemedAtMillis INTEGER NOT NULL, source TEXT NOT NULL DEFAULT 'Purchase', rolledTier INTEGER, PRIMARY KEY(id))")
            connection.execSQL("INSERT INTO reward_templates VALUES ('w1', 'Movie night', 2, 'Low', 'Treat', NULL, 0)")
            connection.execSQL("INSERT INTO redemptions VALUES ('a', 'w1', 'Movie night', 2, 'Low', 50, 3000, 'Purchase', NULL)")
            connection.execSQL("INSERT INTO redemptions VALUES ('b', 'gone', 'Deleted one', 1, 'Low', 10, 4000, 'Roll', 1)")
            connection.execSQL("INSERT INTO redemptions VALUES ('c', NULL, 'No template', 1, 'Low', 10, 5000, 'Purchase', NULL)")
            connection.execSQL("INSERT INTO ledger VALUES ('l1', 100, 1000, 'TaskCompleted', 'p0')")
            connection.execSQL("INSERT INTO ledger VALUES ('l2', -50, 3000, 'RewardRedeemed', 'a')")
            connection.execSQL("INSERT INTO ledger VALUES ('l3', -10, 4000, 'MysteryRoll', 'b')")
            connection.execSQL("INSERT INTO ledger VALUES ('l4', -10, 5000, 'RewardRedeemed', 'c')")
            connection.execSQL("PRAGMA user_version = 5")
        } finally {
            connection.close()
        }
        return file
    }

    @Test
    fun `upgrading from version 5 gives redemptions the description of their reward if it exists`() = runTest {
        val database = Room.databaseBuilder<OpenAdhdDatabase>(createVersion5Database()).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        val byName = store.redemptions.first().associateBy { it.name }
        assertEquals(3, byName.size)
        assertEquals("Treat", byName.getValue("Movie night").description)
        assertEquals("", byName.getValue("Deleted one").description)
        assertEquals("", byName.getValue("No template").description)
        assertEquals(RedemptionSource.Roll, byName.getValue("Deleted one").source)
        assertEquals(30, CoinRules.balance(store.entries.first()))

        // New redemptions keep working, and the stored description no longer follows the reward.
        val reward = store.rewards.first().single()
        store.updateReward(reward.copy(description = "Changed"))
        assertEquals("Treat", store.redemptions.first().first { it.id == "a" }.description)
        database.close()
    }

    @Test
    fun `upgrading to version 7 keeps the data and starts every setting at its default`() = runTest {
        val database = Room.databaseBuilder<OpenAdhdDatabase>(createVersion5Database()).buildOpenAdhdDatabase()
        val store = RoomStore(database)

        assertEquals(Settings.Default, store.settings.first())
        assertEquals(3, store.redemptions.first().size)
        assertEquals(30, CoinRules.balance(store.entries.first()))

        assertNull(store.saveSetting(SettingKey.DebtLimit, "250"))
        assertEquals(250, store.settings.first().rules.debtLimit)
        database.close()
    }
}
