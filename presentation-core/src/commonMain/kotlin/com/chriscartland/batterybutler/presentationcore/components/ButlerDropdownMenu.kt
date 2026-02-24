package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Standard dropdown menu with consistent styling.
 *
 * Wraps Material3 [DropdownMenu] as the single place to customize dropdown behavior
 * across the app — shape, elevation, enter/exit animation, or any other property
 * can be changed here and will apply everywhere automatically.
 */
@Composable
fun ButlerDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        content = content,
    )
}
