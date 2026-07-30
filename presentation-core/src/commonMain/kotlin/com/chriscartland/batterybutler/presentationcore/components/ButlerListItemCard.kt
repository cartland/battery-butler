package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.IconSize
import com.chriscartland.batterybutler.presentationcore.theme.Padding

/**
 * @param contentPadding padding inside the card, around the leading/content/trailing row. Compact
 *   rows tighten the vertical component so the card is only as tall as one line of text.
 * @param leadingSpacing gap between [leading] and [content].
 */
@Composable
fun ButlerListItemCard(
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.cardColors(),
    contentPadding: PaddingValues = PaddingValues(Padding.standard),
    leadingSpacing: Dp = Padding.standard,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val shape = MaterialTheme.shapes.medium
    Card(
        colors = colors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape)
            .clickable { onClick() },
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()

            Spacer(modifier = Modifier.width(leadingSpacing))

            Column(modifier = Modifier.weight(1f)) {
                content()
            }

            if (trailing != null) {
                trailing()
            }
        }
    }
}

@Composable
fun ButlerIconBox(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    size: Dp = ButlerIconBoxDefaults.Size,
    iconSize: Dp = IconSize.Medium,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

object ButlerIconBoxDefaults {
    /** Standard leading icon/photo size for a two-line list row. */
    val Size = 48.dp

    /**
     * Leading icon/photo size for a single-line (compact) list row. Kept at or below the line
     * height of `titleMedium` so the icon never makes the card taller than its text needs.
     */
    val CompactSize = 24.dp
}

@Preview(showBackground = true)
@Composable
fun ButlerListItemCardPreview() {
    BatteryButlerTheme {
        ButlerListItemCard(
            onClick = {},
            leading = {
                ButlerIconBox(
                    icon = Icons.Default.BatteryFull,
                    contentDescription = "Battery",
                )
            },
            trailing = {
                Text(
                    text = "5 days",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        ) {
            Text(
                text = "Example Item",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Supporting text",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ButlerIconBoxPreview() {
    BatteryButlerTheme {
        ButlerIconBox(
            icon = Icons.Default.BatteryFull,
            contentDescription = "Battery",
        )
    }
}
