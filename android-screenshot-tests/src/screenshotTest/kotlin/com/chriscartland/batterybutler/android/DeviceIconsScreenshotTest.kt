package com.chriscartland.batterybutler.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.chriscartland.batterybutler.ui.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.ui.components.DeviceIconMapper
import com.chriscartland.batterybutler.ui.theme.BatteryButlerTheme

@PreviewTest
@Preview(showBackground = true)
@Composable
fun DeviceIconsPreview() {
    BatteryButlerTheme {
        Scaffold(
            topBar = { ButlerCenteredTopAppBar(title = "Device Icons", onBack = {}) },
        ) { innerPadding ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            ) {
                items(DeviceIconMapper.AvailableIcons) { iconName ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(DeviceIconMapper.getContainerColor(iconName)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = DeviceIconMapper.getIcon(iconName),
                                contentDescription = iconName,
                                tint = DeviceIconMapper.getContentColor(iconName),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            text = iconName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
