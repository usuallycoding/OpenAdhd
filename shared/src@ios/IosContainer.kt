package com.usuallycoding.openadhd.data

import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.RoomStore
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createIosContainer(): AppContainer {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val path = requireNotNull(documents?.path) { "Documents directory unavailable" } + "/openadhd.db"
    val database = Room.databaseBuilder<OpenAdhdDatabase>(name = path).buildOpenAdhdDatabase()
    return AppContainer(RoomStore(database))
}
