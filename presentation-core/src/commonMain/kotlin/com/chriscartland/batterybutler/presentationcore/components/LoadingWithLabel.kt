package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.presentationcore.theme.Padding

/**
 * Centered indeterminate spinner with a label underneath, e.g. "Loading devices…".
 *
 * Used in list screens during their initial Loading state. Pair with a screen-
 * specific status string so the label tells the user what's loading, not just
 * that something is.
 */
@Composable
fun LoadingWithLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Padding.standard),
    ) {
        CircularProgressIndicator()
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
