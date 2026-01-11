package com.chriscartland.batterybutler.android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.feature.main.MainScreenContent
import com.chriscartland.batterybutler.feature.main.MainTab

@PreviewTest
@Preview(showBackground = true)
@Composable
fun MainScreenDevicesTabPreview() {
    MainScreenContent(
        currentTab = MainTab.Devices,
        onTabSelected = {},
        onSettingsClick = {},
        onAddClick = {},
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Devices Content Scaffolding")
        }
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun MainScreenTypesTabPreview() {
    MainScreenContent(
        currentTab = MainTab.Types,
        onTabSelected = {},
        onSettingsClick = {},
        onAddClick = {},
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Types Content Scaffolding")
        }
    }
}
