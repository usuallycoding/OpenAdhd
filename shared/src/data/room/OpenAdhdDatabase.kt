package com.usuallycoding.openadhd.data.room

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

@Database(
    entities = [TemplateEntity::class, PlannedTaskEntity::class, LedgerEntity::class],
    version = 1,
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

fun RoomDatabase.Builder<OpenAdhdDatabase>.buildOpenAdhdDatabase(): OpenAdhdDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
