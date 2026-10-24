package com.usuallycoding.openadhd.data

import android.content.Context
import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase

fun createAndroidContainer(context: Context, files: DataFiles? = null): AppContainer {
    val appContext = context.applicationContext
    val database = Room.databaseBuilder<OpenAdhdDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("openadhd.db").absolutePath,
    ).buildOpenAdhdDatabase()
    return createContainer(database, files)
}
