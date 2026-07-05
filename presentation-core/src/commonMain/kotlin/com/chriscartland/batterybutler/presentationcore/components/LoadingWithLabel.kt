package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
        // verticalScroll gives this a nested scroll connection so a PullToRefreshBox
        // ancestor can detect a pull gesture while Loading is showing (no list yet).
        modifier = modifier.verticalScroll(rememberScrollState()),
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
