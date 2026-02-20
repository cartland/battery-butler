package com.chriscartland.batterybutler.androidscreenshottests

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.androidscreenshottests.util.ScreenshotTestTheme
import com.chriscartland.batterybutler.presentationcore.components.AddItemCardPreview
import com.chriscartland.batterybutler.presentationcore.components.CompositeControlPreview
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItemPreview
import com.chriscartland.batterybutler.presentationcore.components.DeviceTypeIconItemPreview
import com.chriscartland.batterybutler.presentationcore.components.EmptyStateContentPreview
import com.chriscartland.batterybutler.presentationcore.components.ExpandableSelectionControlPreview
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItemPreview

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AddItemCardPreviewTest() {
    ScreenshotTestTheme {
        AddItemCardPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CompositeControlPreviewTest() {
    ScreenshotTestTheme {
        CompositeControlPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HistoryListItemPreviewTest() {
    ScreenshotTestTheme {
        HistoryListItemPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceListItemPreviewTest() {
    ScreenshotTestTheme {
        DeviceListItemPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun DeviceTypeIconItemPreviewTest() {
    ScreenshotTestTheme {
        DeviceTypeIconItemPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyStateContentPreviewTest() {
    ScreenshotTestTheme {
        EmptyStateContentPreview()
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Light")
@Preview(showBackground = true, name = "Dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ExpandableSelectionControlPreviewTest() {
    ScreenshotTestTheme {
        ExpandableSelectionControlPreview()
    }
}
