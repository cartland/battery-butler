package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.presentationfeature.settings.SettingsContent

class SettingsScreenshotTest {
    @Preview(showBackground = true)
    @Composable
    fun SettingsContentPreview() {
        SettingsContent(
            networkMode = NetworkMode.Mock,
            availableNetworkModes = listOf(NetworkMode.Mock),
            onNetworkModeSelected = {},
            onExportData = {},
            onBack = {},
            appVersion = com.chriscartland.batterybutler.domain.model.AppVersion
                .Android("1.0.0", 1),
        )
    }
}
