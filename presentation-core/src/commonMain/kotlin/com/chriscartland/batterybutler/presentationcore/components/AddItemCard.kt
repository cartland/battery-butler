package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

@Composable
fun AddItemCard(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ButlerListItemCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
        leading = {
            ButlerIconBox(
                icon = Icons.Default.Add,
                contentDescription = text,
            )
        },
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AddItemCardPreview() {
    BatteryButlerTheme {
        AddItemCard(
            text = "Add a device",
            onClick = {},
        )
    }
}
