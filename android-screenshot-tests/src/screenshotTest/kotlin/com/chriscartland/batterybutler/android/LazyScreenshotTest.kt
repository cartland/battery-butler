package com.chriscartland.batterybutler.android

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest

class LazyScreenshotTest {
    // ✅ This renders correctly
    @PreviewTest
    @Preview(widthDp = 300, heightDp = 400)
    @Composable
    fun ColumnWorksCorrectly() {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Item 1")
                Text("Item 2")
                Text("Item 3")
            }
        }
    }

    // ❌ This renders BLANK
    @PreviewTest
    @Preview(widthDp = 300, heightDp = 400)
    @Composable
    fun LazyColumnRendersBlank() {
        val items = listOf("Item 1", "Item 2", "Item 3")
        Surface(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(items) { item ->
                    Text(item)
                }
            }
        }
    }
}
