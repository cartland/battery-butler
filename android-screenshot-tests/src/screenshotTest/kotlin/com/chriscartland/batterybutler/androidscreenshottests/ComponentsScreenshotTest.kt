package com.chriscartland.batterybutler.androidscreenshottests



import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.domain.model.BatteryEvent
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.CompositeControl
import com.chriscartland.batterybutler.presentationcore.components.CompositeControlPreview
import com.chriscartland.batterybutler.presentationcore.components.HistoryListItemPreview
import com.chriscartland.batterybutler.presentationcore.components.DeviceListItemPreview
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

@PreviewTest
@Preview(showBackground = true)
@Composable
fun CompositeControlPreviewTest() {
    CompositeControlPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun HistoryListItemPreviewTest() {
    HistoryListItemPreview()
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceListItemPreviewTest() {
    DeviceListItemPreview()
}
