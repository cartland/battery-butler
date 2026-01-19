package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

@Composable
fun CompositeControl(
    label: String,
    isActive: Boolean,
    isAscending: Boolean,
    onClicked: () -> Unit,
    onDirectionToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .height(32.dp)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp)),
        color = containerColor,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isActive) {
                // Direction Toggle (Square)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(32.dp)
                        .clickable { onDirectionToggle() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = if (isAscending) "Ascending" else "Descending",
                        tint = contentColor,
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Vertical Separator
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(borderColor),
                )
            }

            // Main Label + Dropdown (Rectangle)
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .clickable { onClicked() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.padding(start = 4.dp).size(18.dp),
                )
            }
        }
    }
}

@Preview
@Composable
fun CompositeControlPreview() {
    BatteryButlerTheme {
        CompositeControl(
            label = "Battery Level",
            isActive = true,
            isAscending = true,
            onClicked = {},
            onDirectionToggle = {},
        )
    }
}
