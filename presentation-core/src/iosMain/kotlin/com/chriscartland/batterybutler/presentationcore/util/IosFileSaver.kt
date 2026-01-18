package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

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

        val activityController = UIActivityViewController(listOf(fileUrl), null)
        val keyWindow = UIApplication.sharedApplication.keyWindow
        val rootViewController = keyWindow?.rootViewController
        rootViewController?.presentViewController(activityController, true, null)
    }
}
