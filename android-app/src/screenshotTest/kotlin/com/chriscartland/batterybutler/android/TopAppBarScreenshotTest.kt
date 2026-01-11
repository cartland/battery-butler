package com.chriscartland.batterybutler.android

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.ui.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.ui.theme.LocalAiAvailable

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarDefaultPreview() {
    ButlerCenteredTopAppBar(
        title = "Battery Butler",
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarWithBackPreview() {
    ButlerCenteredTopAppBar(
        title = "Detail Screen",
        onBack = {},
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarWithAiActionPreview() {
    CompositionLocalProvider(LocalAiAvailable provides true) {
        ButlerCenteredTopAppBar(
            title = "AI Enabled",
            onBack = {},
        )
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarWithMenuActionsPreview() {
    ButlerCenteredTopAppBar(
        title = "With Actions",
        actions = {
            IconButton(onClick = {}) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        },
    )
}
