package com.chriscartland.batterybutler.presentationcore.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Desktop has no cross-platform equivalent of Android's "sensitive clipboard" flag or iOS's
 * localOnly/expiring pasteboard options -- this is a plain copy. (Some clipboard managers on
 * macOS/Linux honor custom concealment MIME hints, but that's manager-specific, not an OS API.)
 */
class DesktopSecureClipboard : SecureClipboard {
    override fun copySensitive(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }
}
