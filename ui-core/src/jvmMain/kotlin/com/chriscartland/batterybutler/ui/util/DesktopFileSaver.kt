package com.chriscartland.batterybutler.ui.util

import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class DesktopFileSaver : FileSaver {
    override fun saveFile(
        fileName: String,
        content: ByteArray,
    ) {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Save Data"
        fileChooser.selectedFile = File(fileName)
        val filter = FileNameExtensionFilter("JSON Files", "json")
        fileChooser.fileFilter = filter

        val userSelection = fileChooser.showSaveDialog(null)

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            var fileToSave = fileChooser.selectedFile
            if (!fileToSave.absolutePath.endsWith(".json")) {
                fileToSave = File(fileToSave.absolutePath + ".json")
            }
            fileToSave.writeBytes(content)
        }
    }
}
