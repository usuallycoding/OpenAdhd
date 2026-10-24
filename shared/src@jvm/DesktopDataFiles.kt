package com.usuallycoding.openadhd.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException
import javax.swing.SwingUtilities

/** Saves and opens backups with the desktop's native file dialog. */
class DesktopDataFiles : DataFiles {
    override suspend fun save(suggestedName: String, text: String): SaveResult {
        val file = choose(FileDialog.SAVE, "Save backup", suggestedName) ?: return SaveResult.Cancelled
        return try {
            withContext(Dispatchers.IO) { file.writeText(text) }
            SaveResult.Saved
        } catch (_: IOException) {
            SaveResult.Failed
        }
    }

    override suspend fun open(): OpenResult {
        val file = choose(FileDialog.LOAD, "Open backup", null) ?: return OpenResult.Cancelled
        return try {
            OpenResult.Text(withContext(Dispatchers.IO) { file.readText() })
        } catch (_: IOException) {
            OpenResult.Failed
        }
    }

    /** Shows the dialog on the AWT event thread, where it blocks until closed, and returns the chosen file or null. */
    private suspend fun choose(mode: Int, title: String, name: String?): File? {
        val chosen = CompletableDeferred<File?>()
        SwingUtilities.invokeLater {
            val dialog = FileDialog(null as Frame?, title, mode)
            if (name != null) dialog.file = name
            dialog.isVisible = true
            val picked = dialog.file?.let { File(dialog.directory.orEmpty(), it) }
            chosen.complete(picked)
        }
        return chosen.await()
    }
}
