package com.usuallycoding.openadhd.data

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.LoginItem
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

/** The folder under the user's home directory where the desktop app keeps its database and small marker files. */
fun defaultDesktopDirectory(): File = File(System.getProperty("user.home"), ".openadhd")

/**
 * Opens (creating if needed) the database in an app folder under the user's home directory. Reminders are shown
 * through [tray] (title and text of a system notification) when the system has a tray; with none ([tray] null) they
 * appear as messages inside the app. [loginItem] is the macOS login item of the packaged app, or null elsewhere.
 */
fun createDesktopContainer(
    directory: File = defaultDesktopDirectory(),
    files: DataFiles? = DesktopDataFiles(),
    tray: ((title: String, text: String) -> Unit)? = null,
    loginItem: LoginItem? = createLoginItem(),
): AppContainer {
    val notices = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val notifier = DesktopNotifier(tray) { notices.tryEmit(it) }
    val scheduler = DesktopReminderScheduler(notify = notifier::show)
    directory.mkdirs()
    val database = Room.databaseBuilder<OpenAdhdDatabase>(File(directory, "openadhd.db").absolutePath)
        .buildOpenAdhdDatabase()
    return createContainer(database, files, scheduler, notices, loginItem = loginItem)
}
