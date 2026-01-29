package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

@Composable
fun <T> ExpandableSelectionControl(
    title: String,
    currentSelection: T,
    options: List<T>,
    onOptionSelected: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(if (expanded) 180f else 0f)

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header Row (Always visible, handles collapse/expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // Show current value if collapsed
                    if (!expanded) {
                        Text(
                            text = optionLabel(currentSelection),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationState),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Expanded Content (Options)
            if (expanded) {
                // Separator or spacing could go here
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected(option)
                                    expanded = false
                                }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = (option == currentSelection),
                                onClick = {
                                    onOptionSelected(option)
                                    expanded = false
                                },
                            )
                            Text(
                                text = optionLabel(option),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpandableSelectionControlPreview() {
    BatteryButlerTheme {
        ExpandableSelectionControl(
            title = "Network Mode",
            currentSelection = "Local Server",
            options = listOf("Mock Data", "Local Server", "Production"),
            onOptionSelected = {},
            optionLabel = { it },
        )
    }
}
