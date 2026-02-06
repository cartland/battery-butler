package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.BatchOperationResult

/**
 * Displays a single batch operation result with appropriate styling.
 *
 * Uses emoji prefixes and color coding:
 * - Progress: 🤖 with default text color
 * - Success: ✅ with primary color
 * - Error: ❌ with error color
 */
@Composable
fun BatchOperationResultItem(
    result: BatchOperationResult,
    modifier: Modifier = Modifier,
) {
    val (text, color) = when (result) {
        is BatchOperationResult.Progress ->
            "🤖 ${result.message}" to MaterialTheme.colorScheme.onSurface
        is BatchOperationResult.Success ->
            "✅ ${result.message}" to MaterialTheme.colorScheme.primary
        is BatchOperationResult.Error ->
            "❌ ${result.error.message}" to MaterialTheme.colorScheme.error
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier.padding(vertical = 4.dp),
    )
}
