package com.usuallycoding.openadhd

import com.usuallycoding.openadhd.data.DataFiles
import com.usuallycoding.openadhd.data.OpenResult
import com.usuallycoding.openadhd.data.SaveResult

/** A [DataFiles] for tests: remembers what was saved, and answers [save] with [nextSave] and [open] with [nextOpen]. */
class FakeDataFiles(
    var nextOpen: OpenResult = OpenResult.Cancelled,
    var nextSave: SaveResult = SaveResult.Saved,
) : DataFiles {
    val saved = mutableListOf<Pair<String, String>>()
    var opened = 0
        private set

    constructor(text: String) : this(OpenResult.Text(text))

    override suspend fun save(suggestedName: String, text: String): SaveResult {
        if (nextSave == SaveResult.Saved) saved += suggestedName to text
        return nextSave
    }

    override suspend fun open(): OpenResult {
        opened++
        return nextOpen
    }
}
