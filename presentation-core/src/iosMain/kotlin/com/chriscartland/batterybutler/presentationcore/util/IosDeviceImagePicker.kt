package com.chriscartland.batterybutler.presentationcore.util

import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSItemProvider
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter.Companion.imagesFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject

/**
 * Uses `PHPickerViewController` -- no photo-library permission prompt, per
 * `docs/DEVICE_IMAGES.md` §6C. Resolves the presenting window at call time (the same
 * no-held-reference idiom [IosFileLoader] uses for `UIDocumentPickerViewController`), so there's
 * no bind/unbind lifecycle to manage.
 */
class IosDeviceImagePicker : DeviceImagePicker {
    private var pendingCallback: ((ByteArray?) -> Unit)? = null

    override fun pickImage(onResult: (ByteArray?) -> Unit) {
        pendingCallback = onResult

        val configuration = PHPickerConfiguration()
        configuration.filter = imagesFilter()
        configuration.selectionLimit = 1L

        val picker = PHPickerViewController(configuration = configuration)
        picker.delegate = PhotoPickerDelegate(this)

        val keyWindow = UIApplication.sharedApplication.keyWindow
        keyWindow?.rootViewController?.presentViewController(picker, animated = true, completion = null)
    }

    internal fun handlePickedResult(result: PHPickerResult?) {
        val itemProvider = result?.itemProvider
        if (itemProvider == null || !itemProvider.hasItemConformingToTypeIdentifier(UTTypeImage.identifier)) {
            deliverResult(null)
            return
        }
        itemProvider.loadDataRepresentationForTypeIdentifier(UTTypeImage.identifier) { data: NSData?, _: NSError? ->
            deliverResult(data?.toByteArray())
        }
    }

    internal fun handleCancelled() {
        deliverResult(null)
    }

    private fun deliverResult(bytes: ByteArray?) {
        val callback = pendingCallback
        pendingCallback = null
        callback?.invoke(bytes)
    }
}

private class PhotoPickerDelegate(
    private val picker: IosDeviceImagePicker,
) : NSObject(),
    PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            this.picker.handleCancelled()
        } else {
            this.picker.handlePickedResult(result)
        }
    }
}
