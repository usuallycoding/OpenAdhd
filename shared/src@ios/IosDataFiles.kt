package com.usuallycoding.openadhd.data

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import kotlin.coroutines.resume

/** Saves and opens backups with the system's document picker, presented over the app's top view controller. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosDataFiles : DataFiles {
    /** UIKit keeps its delegate weakly, so the one in use is held here until the picker is done. */
    private var delegate: PickerDelegate? = null

    override suspend fun save(suggestedName: String, text: String): SaveResult {
        val path = NSTemporaryDirectory() + suggestedName
        val written = NSString.create(string = text)
            .writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!written) return SaveResult.Failed
        val picked = present { UIDocumentPickerViewController(forExportingURLs = listOf(NSURL.fileURLWithPath(path)), asCopy = true) }
        return if (picked != null) SaveResult.Saved else SaveResult.Cancelled
    }

    override suspend fun open(): OpenResult {
        val urls = present { UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON), asCopy = true) }
            ?: return OpenResult.Cancelled
        val url = urls.firstOrNull() as? NSURL ?: return OpenResult.Cancelled
        val opened = url.startAccessingSecurityScopedResource()
        try {
            val text = NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
            return if (text == null) OpenResult.Failed else OpenResult.Text(text)
        } finally {
            if (opened) url.stopAccessingSecurityScopedResource()
        }
    }

    /** Presents the picker and waits for its result: the chosen URLs, or null when the user cancelled. */
    private suspend fun present(makePicker: () -> UIDocumentPickerViewController): List<*>? =
        suspendCancellableCoroutine { continuation ->
            val picker = makePicker()
            val pickerDelegate = PickerDelegate { urls ->
                delegate = null
                if (continuation.isActive) continuation.resume(urls)
            }
            delegate = pickerDelegate
            picker.delegate = pickerDelegate
            val presenter = topViewController()
            if (presenter == null) {
                delegate = null
                continuation.resume(null)
            } else {
                presenter.presentViewController(picker, animated = true, completion = null)
            }
            continuation.invokeOnCancellation { delegate = null }
        }

    private fun topViewController(): UIViewController? {
        var top = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (top?.presentedViewController != null) top = top.presentedViewController
        return top
    }

    private class PickerDelegate(private val onResult: (List<*>?) -> Unit) : NSObject(), UIDocumentPickerDelegateProtocol {
        override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
            onResult(didPickDocumentsAtURLs)
        }

        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
            onResult(null)
        }
    }
}
