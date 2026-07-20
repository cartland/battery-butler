package com.chriscartland.batterybutler.presentationcore.util

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopDeviceImagePicker : DeviceImagePicker {
    override fun pickImage(onResult: (ByteArray?) -> Unit) {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Choose Photo"
        fileChooser.fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")

        val result = fileChooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            onResult(fileChooser.selectedFile.readBytes())
        } else {
            onResult(null)
        }
    }
}
