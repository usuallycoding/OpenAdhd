package com.usuallycoding.openadhd.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * How a platform asks the app to show one of today's entries, such as when the user taps its reminder notification.
 * The request is kept until the app takes it, so a tap that launches the app, before anything is on screen, is not lost.
 */
class EntryRequests {
    private val _pending = MutableStateFlow<String?>(null)

    /** The id of the entry to show, or null when nothing is waiting. */
    val pending: StateFlow<String?> = _pending

    /** Asks for [entryId] to be shown; a newer request replaces one that has not been taken yet. */
    fun open(entryId: String) {
        _pending.value = entryId
    }

    /** Marks [entryId] as taken. A newer request for another entry is left waiting. */
    fun consume(entryId: String) {
        _pending.compareAndSet(entryId, null)
    }
}
