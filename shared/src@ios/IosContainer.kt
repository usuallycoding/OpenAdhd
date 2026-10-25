package com.usuallycoding.openadhd.data

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.EntryRequests
import com.usuallycoding.openadhd.domain.ReminderScheduler
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createIosContainer(
    files: DataFiles? = IosDataFiles(),
    // Shared by the scheduler, which hears about a tapped notification, and the container, which hands it to the app.
    entryRequests: EntryRequests = EntryRequests(),
    reminders: ReminderScheduler? = IosReminderScheduler(entryRequests),
): AppContainer {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val path = requireNotNull(documents?.path) { "Documents directory unavailable" } + "/openadhd.db"
    val database = Room.databaseBuilder<OpenAdhdDatabase>(name = path).buildOpenAdhdDatabase()
    return createContainer(database, files, reminders, entryRequests = entryRequests)
}
