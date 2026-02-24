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
            Logger.i { "Exported data copied to clipboard: $text" }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to copy to clipboard: ${e.message}" }
            Logger.i { "Exported data: $text" }
        }
    }
}
