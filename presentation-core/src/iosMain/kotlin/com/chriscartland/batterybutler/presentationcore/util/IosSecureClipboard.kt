package com.chriscartland.batterybutler.presentationcore.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.UIKit.UIPasteboard
import platform.UIKit.UIPasteboardOptionExpirationDate
import platform.UIKit.UIPasteboardOptionLocalOnly

class IosSecureClipboard : SecureClipboard {
    @OptIn(ExperimentalForeignApi::class)
    override fun copySensitive(text: String) {
        // localOnly keeps this off Universal Clipboard (no sync to other Apple devices);
        // expirationDate clears it from the pasteboard automatically after a short window --
        // the same treatment iOS gives a copied password or one-time code.
        UIPasteboard.generalPasteboard.setItems(
            listOf(mapOf(PLAIN_TEXT_UTI to text)),
            options =
                mapOf(
                    UIPasteboardOptionLocalOnly to true,
                    UIPasteboardOptionExpirationDate to NSDate.dateWithTimeIntervalSinceNow(EXPIRY_SECONDS),
                ),
        )
    }

    private companion object {
        const val PLAIN_TEXT_UTI = "public.plain-text"
        const val EXPIRY_SECONDS = 60.0
    }
}
