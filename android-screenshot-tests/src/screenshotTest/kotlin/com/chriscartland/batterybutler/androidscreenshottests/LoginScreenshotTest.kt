package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationfeature.login.LoginContentAuthenticatingPreview
import com.chriscartland.batterybutler.presentationfeature.login.LoginContentErrorPreview
import com.chriscartland.batterybutler.presentationfeature.login.LoginContentNotConfiguredPreview
import com.chriscartland.batterybutler.presentationfeature.login.LoginContentUnauthenticatedPreview

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LoginContentUnauthenticatedPreviewTest() {
    ScreenshotTestTheme {
        LoginContentUnauthenticatedPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LoginContentAuthenticatingPreviewTest() {
    ScreenshotTestTheme {
        LoginContentAuthenticatingPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LoginContentNotConfiguredPreviewTest() {
    ScreenshotTestTheme {
        LoginContentNotConfiguredPreview()
    }
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun LoginContentErrorPreviewTest() {
    ScreenshotTestTheme {
        LoginContentErrorPreview()
    }
}
