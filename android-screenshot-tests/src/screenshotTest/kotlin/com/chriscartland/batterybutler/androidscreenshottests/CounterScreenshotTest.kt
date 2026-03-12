package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.experimental.presentationcore.CounterContentActivePreview
import com.chriscartland.batterybutler.experimental.presentationcore.CounterContentErrorPreview
import com.chriscartland.batterybutler.experimental.presentationcore.CounterContentIdlePreview
import com.chriscartland.batterybutler.experimental.presentationcore.CounterContentLoadingPreview

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CounterIdlePreviewTest() {
    ScreenshotTestTheme {
        CounterContentIdlePreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CounterLoadingPreviewTest() {
    ScreenshotTestTheme {
        CounterContentLoadingPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CounterActivePreviewTest() {
    ScreenshotTestTheme {
        CounterContentActivePreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CounterErrorPreviewTest() {
    ScreenshotTestTheme {
        CounterContentErrorPreview()
    }
}
