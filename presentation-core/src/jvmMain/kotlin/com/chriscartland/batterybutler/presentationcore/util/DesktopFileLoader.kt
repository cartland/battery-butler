package com.chriscartland.batterybutler.presentationcore.util

import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopFileLoader : FileLoader {
    override fun loadFile(onResult: (ByteArray?) -> Unit) {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Import Data"
        fileChooser.fileFilter = FileNameExtensionFilter("JSON Files", "json")

        val result = fileChooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            onResult(fileChooser.selectedFile.readBytes())
        } else {
            onResult(null)
        }
    }
}
