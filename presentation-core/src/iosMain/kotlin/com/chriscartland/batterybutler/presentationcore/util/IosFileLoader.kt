package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.create
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
        // Start accessing security-scoped resource.
        val accessing = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.create(contentsOfURL = url) ?: return null
            val length = data.length.toInt()
            if (length == 0) return ByteArray(0)
            val bytes = ByteArray(length)
            bytes.usePinned { pinned ->
                data.getBytes(pinned.addressOf(0), length.toULong())
            }
            return bytes
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
