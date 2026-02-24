package com.chriscartland.batterybutler.presentationcore.util

import co.touchlab.kermit.Logger
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class DesktopShareHandler : ShareHandler {
    override fun shareText(text: String) {
        try {
            val selection = StringSelection(text)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, selection)
            Logger.i(TAG) { "Exported data copied to clipboard (length: ${text.length})" }
        } catch (e: Exception) {
            Logger.e(TAG, e) { "Failed to copy to clipboard" }
        }
    }

    companion object {
        private const val TAG = "DesktopShareHandler"
    }
}
