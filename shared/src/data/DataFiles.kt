package com.usuallycoding.openadhd.data

/**
 * The only platform-specific part of backups: asking the user for a file through the system's own dialog and
 * reading or writing its text. Each platform has one implementation; where there is none the app has no
 * [DataFiles] and hides Export and Import.
 */
interface DataFiles {
    /** Lets the user choose where to save [text], offering [suggestedName]. */
    suspend fun save(suggestedName: String, text: String): SaveResult

    /** Lets the user choose a file and reads its text. */
    suspend fun open(): OpenResult
}

enum class SaveResult { Saved, Cancelled, Failed }

sealed interface OpenResult {
    data class Text(val text: String) : OpenResult
    data object Cancelled : OpenResult
    data object Failed : OpenResult
}
