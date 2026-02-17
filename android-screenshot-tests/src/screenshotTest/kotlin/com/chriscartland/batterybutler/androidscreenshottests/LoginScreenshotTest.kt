package com.chriscartland.batterybutler.androidscreenshottests

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.login.LoginContentAuthenticatingPreview
import com.chriscartland.batterybutler.presentationfeature.login.LoginContentErrorPreview
import com.chriscartland.batterybutler.presentationfeature.login.LoginContentNotConfiguredPreview
import com.chriscartland.batterybutler.presentationfeature.login.LoginContentUnauthenticatedPreview

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginContentUnauthenticatedPreviewTest() {
    ScreenshotTestTheme {
        LoginContentUnauthenticatedPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginContentAuthenticatingPreviewTest() {
    ScreenshotTestTheme {
        LoginContentAuthenticatingPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginContentNotConfiguredPreviewTest() {
    ScreenshotTestTheme {
        LoginContentNotConfiguredPreview()
    }
}

@PreviewTest
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LoginContentErrorPreviewTest() {
    ScreenshotTestTheme {
        LoginContentErrorPreview()
    }
}
