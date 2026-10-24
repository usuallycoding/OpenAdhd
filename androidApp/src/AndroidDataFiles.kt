package com.usuallycoding.openadhd

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.usuallycoding.openadhd.data.DataFiles
import com.usuallycoding.openadhd.data.OpenResult
import com.usuallycoding.openadhd.data.SaveResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

/** Saves and opens backups with the system's document picker (the Storage Access Framework). */
class AndroidDataFiles(private val activity: ComponentActivity) : DataFiles {
    private var requests = 0

    override suspend fun save(suggestedName: String, text: String): SaveResult {
        val uri = pick(ActivityResultContracts.CreateDocument("application/json"), suggestedName) ?: return SaveResult.Cancelled
        return try {
            withContext(Dispatchers.IO) {
                val stream = activity.contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Cannot open $uri")
                stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            }
            SaveResult.Saved
        } catch (_: IOException) {
            SaveResult.Failed
        }
    }

    override suspend fun open(): OpenResult {
        val uri = pick(ActivityResultContracts.OpenDocument(), arrayOf("application/json", "text/plain", "*/*")) ?: return OpenResult.Cancelled
        return try {
            val text = withContext(Dispatchers.IO) {
                activity.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            }
            if (text == null) OpenResult.Failed else OpenResult.Text(text)
        } catch (_: IOException) {
            OpenResult.Failed
        }
    }

    /** Launches the picker and waits for the chosen Uri; null when the user backs out. */
    private suspend fun <I> pick(contract: ActivityResultContract<I, Uri?>, input: I): Uri? =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val launcher = activity.activityResultRegistry.register("openadhd-files-${requests++}", contract) { uri ->
                    if (continuation.isActive) continuation.resume(uri)
                }
                continuation.invokeOnCancellation { launcher.unregister() }
                launcher.launch(input)
            }
        }
}
