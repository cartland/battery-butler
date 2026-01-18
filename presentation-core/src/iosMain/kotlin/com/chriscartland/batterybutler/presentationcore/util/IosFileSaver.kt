package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityItemSourceProtocol
import platform.UIKit.UIActivityType
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.darwin.NSObject

class IosFileSaver : FileSaver {
    @OptIn(ExperimentalForeignApi::class)
    override fun saveFile(
        fileName: String,
        content: ByteArray,
    ) {
        val fileManager = NSFileManager.defaultManager
        val cacheDir = fileManager.URLsForDirectory(NSCachesDirectory, NSUserDomainMask).first() as NSURL
        val fileUrl = cacheDir.URLByAppendingPathComponent(fileName) ?: return

        content.usePinned { pinned ->
            val data = NSData.dataWithBytes(pinned.addressOf(0), content.size.toULong())
            data.writeToURL(fileUrl, true)
        }

        val itemSource = FileActivityItemSource(fileUrl)
        val activityController = UIActivityViewController(listOf(itemSource), null)
        val keyWindow = UIApplication.sharedApplication.keyWindow
        val rootViewController = keyWindow?.rootViewController
        rootViewController?.presentViewController(activityController, true, null)
    }
}

class FileActivityItemSource(
    private val fileUrl: NSURL,
) : NSObject(),
    UIActivityItemSourceProtocol {
    override fun activityViewControllerPlaceholderItem(activityViewController: UIActivityViewController): Any = fileUrl

    @ObjCSignatureOverride
    override fun activityViewController(
        activityViewController: UIActivityViewController,
        itemForActivityType: UIActivityType?,
    ): Any? = fileUrl

    @ObjCSignatureOverride
    override fun activityViewController(
        activityViewController: UIActivityViewController,
        subjectForActivityType: UIActivityType?,
    ): String = fileUrl.lastPathComponent ?: "Export"

    @ObjCSignatureOverride
    override fun activityViewController(
        activityViewController: UIActivityViewController,
        dataTypeIdentifierForActivityType: UIActivityType?,
    ): String = "public.json"
}
