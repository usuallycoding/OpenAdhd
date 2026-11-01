package com.usuallycoding.openadhd.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.room.Room
import com.usuallycoding.openadhd.data.room.OpenAdhdDatabase
import com.usuallycoding.openadhd.data.room.buildOpenAdhdDatabase
import com.usuallycoding.openadhd.domain.AppVersion
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
    return createContainer(database, files, reminders, versionLabel = installedVersionLabel(appContext))
}

/** The version line from the installed package: its version name and version code, or "dev" if they cannot be read. */
private fun installedVersionLabel(context: Context): String = runCatching {
    val packages = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packages.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        packages.getPackageInfo(context.packageName, 0)
    }
    val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        info.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        info.versionCode.toLong()
    }
    AppVersion.label(info.versionName, code.toString())
}.getOrDefault(AppVersion.Unpackaged)
