package com.chriscartland.batterybutler.presentation.core.util

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

class IosShareHandler : ShareHandler {
    override fun shareText(text: String) {
        val controller = UIActivityViewController(listOf(text), null)
        val keyWindow = UIApplication.sharedApplication.keyWindow
        val rootViewController = keyWindow?.rootViewController
        rootViewController?.presentViewController(controller, true, null)
    }
}
