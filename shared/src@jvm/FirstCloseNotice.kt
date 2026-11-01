package com.usuallycoding.openadhd.data

import java.io.File

/**
 * Remembers, with an empty marker file in [directory], whether the user has already been told that closing the window
 * leaves the app running in the tray. The marker lives next to the database and not in the settings, so a backup or
 * Delete all data neither carries nor clears it.
 */
class FirstCloseNotice(private val directory: File) {
    /** True the first time it is asked, and false ever after, including when the marker cannot be written. */
    fun shouldShow(): Boolean {
        directory.mkdirs()
        return runCatching { File(directory, MarkerName).createNewFile() }.getOrDefault(false)
    }

    private companion object {
        const val MarkerName = "tray-notice-shown"
    }
}
