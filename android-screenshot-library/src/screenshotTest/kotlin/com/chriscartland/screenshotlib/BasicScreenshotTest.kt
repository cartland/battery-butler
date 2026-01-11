package com.chriscartland.screenshotlib

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true)
@Composable
fun BasicTextPreview() {
    Text("Hello World from Library Module - UPDATED")
}
