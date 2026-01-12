package com.chriscartland.batterybutler.presentation.core.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class DesktopShareHandler : ShareHandler {
    override fun shareText(text: String) {
        try {
            val selection = StringSelection(text)
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(selection, selection)
            println("Exported data copied to clipboard: $text")
        } catch (e: Exception) {
            println("Failed to copy to clipboard: ${e.message}")
            println("Exported data: $text")
        }
    }
}
