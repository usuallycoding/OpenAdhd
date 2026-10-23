package com.usuallycoding.openadhd.data.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [
        TemplateEntity::class,
        TemplateSubtaskEntity::class,
        PlannedTaskEntity::class,
        PlannedSubtaskEntity::class,
        LedgerEntity::class,
        RewardTemplateEntity::class,
        RedemptionEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@ConstructedBy(OpenAdhdDatabaseConstructor::class)
abstract class OpenAdhdDatabase : RoomDatabase() {
    abstract fun dao(): OpenAdhdDao
}

// Room's compiler generates the actual for each platform.
@Suppress("KotlinNoActualForExpect")
expect object OpenAdhdDatabaseConstructor : RoomDatabaseConstructor<OpenAdhdDatabase> {
    override fun initialize(): OpenAdhdDatabase
}

/**
 * Version 1 had no descriptions or subtasks and computed each reward from the tier when completing.
 * Old entries get their reward backfilled from the tier table as it was then (5/10/20/40), so they keep
 * their value; ledger rows are not touched, so the balance does not change.
 */
val Migration1To2: Migration = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE templates ADD COLUMN description TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE planned_tasks ADD COLUMN description TEXT NOT NULL DEFAULT ''")
        connection.execSQL("ALTER TABLE planned_tasks ADD COLUMN reward INTEGER NOT NULL DEFAULT 0")
        connection.execSQL(
            "UPDATE planned_tasks SET reward = CASE tier WHEN 1 THEN 5 WHEN 2 THEN 10 WHEN 3 THEN 20 WHEN 4 THEN 40 ELSE 0 END",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS template_subtasks " +
                "(id TEXT NOT NULL, templateId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, PRIMARY KEY(id))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS planned_subtasks " +
                "(id TEXT NOT NULL, plannedTaskId TEXT NOT NULL, position INTEGER NOT NULL, title TEXT NOT NULL, " +
                "reward INTEGER NOT NULL, completedAtMillis INTEGER, PRIMARY KEY(id))",
        )
    }
}

/** Version 3 adds rewards and redemptions. It only creates tables, so no existing data is touched. */
val Migration2To3: Migration = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS reward_templates " +
                "(id TEXT NOT NULL, name TEXT NOT NULL, tier INTEGER NOT NULL, energy TEXT NOT NULL, " +
                "description TEXT NOT NULL, customPrice INTEGER, PRIMARY KEY(id))",
        )
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS redemptions " +
                "(id TEXT NOT NULL, templateId TEXT, name TEXT NOT NULL, tier INTEGER NOT NULL, " +
                "energy TEXT NOT NULL, price INTEGER NOT NULL, redeemedAtMillis INTEGER NOT NULL, PRIMARY KEY(id))",
        )
    }
}

fun RoomDatabase.Builder<OpenAdhdDatabase>.buildOpenAdhdDatabase(): OpenAdhdDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(Migration1To2, Migration2To3)
        .build()
