package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject

class IosFileLoader : FileLoader {
    private var pendingCallback: ((ByteArray?) -> Unit)? = null

    override fun loadFile(onResult: (ByteArray?) -> Unit) {
        pendingCallback = onResult

        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeJSON),
        )
        picker.allowsMultipleSelection = false
        picker.delegate = DocumentPickerDelegate(this)

        val keyWindow = UIApplication.sharedApplication.keyWindow
        keyWindow?.rootViewController?.presentViewController(picker, animated = true, completion = null)
    }

    internal fun handlePickedUrl(url: NSURL?) {
        val callback = pendingCallback
        pendingCallback = null
        if (url == null) {
            callback?.invoke(null)
            return
        }
        val data = readFileData(url)
        callback?.invoke(data)
    }

    internal fun handleCancelled() {
        val callback = pendingCallback
        pendingCallback = null
        callback?.invoke(null)
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readFileData(url: NSURL): ByteArray? {
        val accessing = url.startAccessingSecurityScopedResource()
        try {
            val content = NSString.stringWithContentsOfURL(
                url = url,
                encoding = NSUTF8StringEncoding,
                error = null,
            ) ?: return null
            return content.encodeToByteArray()
        } finally {
            if (accessing) {
                url.stopAccessingSecurityScopedResource()
            }
        }
    }
}

private class DocumentPickerDelegate(
    private val loader: IosFileLoader,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
        loader.handlePickedUrl(url)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        loader.handleCancelled()
    }
}
