package com.usuallycoding.openadhd.data

import android.content.Context
import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.ReminderScheduler

fun createAndroidContainer(
    context: Context,
    files: DataFiles? = null,
    reminders: ReminderScheduler? = null,
): AppContainer {
    val appContext = context.applicationContext
    val database = Room.databaseBuilder<OpenAdhdDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("openadhd.db").absolutePath,
    ).buildOpenAdhdDatabase()
    return createContainer(database, files, reminders)
}
