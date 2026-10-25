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
        SettingEntity::class,
    ],
    version = 10,
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

/** Version 4 records how a redemption was obtained. Earlier redemptions become plain purchases. */
val Migration3To4: Migration = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE redemptions ADD COLUMN source TEXT NOT NULL DEFAULT 'Purchase'")
        connection.execSQL("ALTER TABLE redemptions ADD COLUMN rolledTier INTEGER")
    }
}

/**
 * Version 5 stores the order of task templates, reward templates and Today entries. Each list gets its
 * positions from the order it already had (rowid, per date for Today entries), so nothing moves.
 */
val Migration4To5: Migration = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        for (table in listOf("templates", "reward_templates", "planned_tasks")) {
            connection.execSQL("ALTER TABLE $table ADD COLUMN position INTEGER NOT NULL DEFAULT 0")
        }
        connection.execSQL(
            "UPDATE templates SET position = (SELECT COUNT(*) FROM templates AS earlier WHERE earlier.rowid < templates.rowid)",
        )
        connection.execSQL(
            "UPDATE reward_templates SET position = " +
                "(SELECT COUNT(*) FROM reward_templates AS earlier WHERE earlier.rowid < reward_templates.rowid)",
        )
        connection.execSQL(
            "UPDATE planned_tasks SET position = (SELECT COUNT(*) FROM planned_tasks AS earlier " +
                "WHERE earlier.date = planned_tasks.date AND earlier.rowid < planned_tasks.rowid)",
        )
    }
}

/**
 * Version 6 keeps the reward's description on each redemption, so history does not change when a reward is
 * edited or deleted. Earlier redemptions get the description of their reward if it still exists, which may
 * differ from what it said at the time, and none otherwise. Ledger rows are not touched.
 */
val Migration5To6: Migration = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE redemptions ADD COLUMN description TEXT NOT NULL DEFAULT ''")
        connection.execSQL(
            "UPDATE redemptions SET description = COALESCE(" +
                "(SELECT description FROM reward_templates WHERE reward_templates.id = redemptions.templateId), '')",
        )
    }
}

/** Version 7 adds the settings table. It only creates a table, so no existing data is touched and every setting starts at its default. */
val Migration6To7: Migration = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS settings (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
    }
}

/** Version 8 adds an optional reminder to Today entries. Two nullable columns, so existing entries simply have none. */
val Migration7To8: Migration = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE planned_tasks ADD COLUMN reminderAtMillis INTEGER")
        connection.execSQL("ALTER TABLE planned_tasks ADD COLUMN reminderRepeatMinutes INTEGER")
    }
}

/**
 * Version 9 adds a schedule, a reminder and the last generated day to task templates. All columns are nullable or
 * default to "no schedule", so existing templates stay manual-only.
 */
val Migration8To9: Migration = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE templates ADD COLUMN scheduleKind TEXT NOT NULL DEFAULT 'None'")
        connection.execSQL("ALTER TABLE templates ADD COLUMN scheduleStart TEXT")
        connection.execSQL("ALTER TABLE templates ADD COLUMN scheduleEnd TEXT")
        connection.execSQL("ALTER TABLE templates ADD COLUMN scheduleDays INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE templates ADD COLUMN reminderMinuteOfDay INTEGER")
        connection.execSQL("ALTER TABLE templates ADD COLUMN reminderRepeatMinutes INTEGER")
        connection.execSQL("ALTER TABLE templates ADD COLUMN lastGenerated TEXT")
    }
}

/** Version 10 adds the kind of a reminder, a notification or an alarm, to Today entries and templates. Existing reminders stay notifications. */
val Migration9To10: Migration = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE planned_tasks ADD COLUMN reminderKind TEXT NOT NULL DEFAULT 'Notification'")
        connection.execSQL("ALTER TABLE templates ADD COLUMN reminderKind TEXT NOT NULL DEFAULT 'Notification'")
    }
}

fun RoomDatabase.Builder<OpenAdhdDatabase>.buildOpenAdhdDatabase(): OpenAdhdDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5, Migration5To6, Migration6To7, Migration7To8, Migration8To9, Migration9To10)
        .build()
