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
    ],
    version = 2,
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

fun RoomDatabase.Builder<OpenAdhdDatabase>.buildOpenAdhdDatabase(): OpenAdhdDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(Migration1To2)
        .build()
