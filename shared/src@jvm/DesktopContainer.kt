package com.usuallycoding.openadhd.data

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import kotlinx.coroutines.flow.MutableSharedFlow
import java.io.File

/** Opens (creating if needed) the database in an app folder under the user's home directory. */
fun createDesktopContainer(
    directory: File = File(System.getProperty("user.home"), ".openadhd"),
    files: DataFiles? = DesktopDataFiles(),
): AppContainer {
    // Reminders go through the system tray; where it is missing they appear as messages inside the app.
    val notices = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val notifier = DesktopNotifier { notices.tryEmit(it) }
    val scheduler = DesktopReminderScheduler(notify = notifier::show, onPending = notifier::setPending)
    directory.mkdirs()
    val database = Room.databaseBuilder<OpenAdhdDatabase>(File(directory, "openadhd.db").absolutePath)
        .buildOpenAdhdDatabase()
    return createContainer(database, files, scheduler, notices)
}
