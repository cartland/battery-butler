package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DensityLarge
import androidx.compose.material.icons.filled.DensitySmall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.IconSize
import com.chriscartland.batterybutler.presentationcore.theme.Padding

/** Shared height for filter-row controls, so [CompositeControl] and [IconControl] line up. */
private val ControlSize = 32.dp

@Composable
fun CompositeControl(
    label: String,
    isActive: Boolean,
    isAscending: Boolean,
    onClicked: () -> Unit,
    onDirectionToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val containerColor = Color.Transparent
    val contentColor = MaterialTheme.colorScheme.onSurface
    val shape = MaterialTheme.shapes.small

    Surface(
        modifier = modifier
            .height(ControlSize)
            .border(1.dp, borderColor, shape)
            .clip(shape),
        color = containerColor,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isActive) {
                // Direction Toggle (Square)
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(ControlSize)
                        .clickable { onDirectionToggle() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isAscending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = if (isAscending) "Ascending" else "Descending",
                        tint = contentColor,
                        modifier = Modifier.size(IconSize.ExtraSmall),
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
                    .padding(horizontal = Padding.medium),
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
                    modifier = Modifier.padding(start = Padding.extraSmall).size(IconSize.Small),
                )
            }
        }
    }
}

/**
 * Icon-only sibling of [CompositeControl]: same height, border, and shape, but square and with no
 * text label. For a binary display toggle (e.g. list density) where a labelled dropdown would cost
 * more filter-row width than a two-way choice is worth.
 *
 * The caller decides what the icon means. The convention in the Home filter row is that the icon
 * shows the **current** state while [contentDescription] describes the **action** a tap performs.
 */
@Composable
fun IconControl(
    icon: ImageVector,
    contentDescription: String,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val contentColor = MaterialTheme.colorScheme.onSurface
    val shape = MaterialTheme.shapes.small

    Surface(
        modifier = modifier
            .size(ControlSize)
            .border(1.dp, borderColor, shape)
            .clip(shape),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.clickable { onClicked() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(IconSize.Small),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CompositeControlPreview() {
    BatteryButlerTheme {
        Surface {
            CompositeControl(
                label = "Battery Level",
                isActive = true,
                isAscending = true,
                onClicked = {},
                onDirectionToggle = {},
            )
        }
    }
}

/** Both density icons side by side: expanded (spaced rows) then compact (dense rows). */
@Preview(showBackground = true)
@Composable
fun IconControlPreview() {
    BatteryButlerTheme {
        Surface {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Padding.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconControl(
                    icon = Icons.Default.DensityLarge,
                    contentDescription = "Expanded view",
                    onClicked = {},
                )
                IconControl(
                    icon = Icons.Default.DensitySmall,
                    contentDescription = "Compact view",
                    onClicked = {},
                )
            }
        }
    }
}
