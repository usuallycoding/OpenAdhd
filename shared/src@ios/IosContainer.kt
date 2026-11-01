package com.usuallycoding.openadhd.data

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.EntryRequests
import com.usuallycoding.openadhd.domain.ReminderScheduler
import kotlinx.cinterop.ExperimentalForeignApi
import com.usuallycoding.openadhd.domain.AppVersion
import platform.Foundation.NSBundle
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createIosContainer(
    files: DataFiles? = IosDataFiles(),
    // Shared by the scheduler, which hears about a tapped notification, and the container, which hands it to the app.
    entryRequests: EntryRequests = IosBridge.entryRequests,
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
    return createContainer(
        database, files, reminders, entryRequests = entryRequests,
        versionLabel = versionLabelOf(NSBundle.mainBundle.infoDictionary),
    )
}

/** The version line from the app's Info.plist: its short version string and its bundle version, or "dev" if there is none. */
internal fun versionLabelOf(info: Map<*, *>?): String =
    AppVersion.label(info?.get("CFBundleShortVersionString") as? String, info?.get("CFBundleVersion") as? String)
