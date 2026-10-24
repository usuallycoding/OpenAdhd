package com.usuallycoding.openadhd.data

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import java.io.File

/** Opens (creating if needed) the database in an app folder under the user's home directory. */
fun createDesktopContainer(
    directory: File = File(System.getProperty("user.home"), ".openadhd"),
    files: DataFiles? = DesktopDataFiles(),
): AppContainer {
    directory.mkdirs()
    val database = Room.databaseBuilder<OpenAdhdDatabase>(File(directory, "openadhd.db").absolutePath)
        .buildOpenAdhdDatabase()
    return createContainer(database, files)
}
