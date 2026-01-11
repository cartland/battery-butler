package com.chriscartland.batterybutler.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.ui.components.ButlerCenteredTopAppBar

@PreviewTest
@Preview(showBackground = true)
@Composable
fun TopAppBarPreview() {
    ButlerCenteredTopAppBar(
        title = "Screenshot Test"
    )
}
