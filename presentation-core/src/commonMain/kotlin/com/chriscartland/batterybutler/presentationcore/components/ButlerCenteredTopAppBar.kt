package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.chriscartland.batterybutler.presentationcore.theme.LocalAiAction
import com.chriscartland.batterybutler.presentationcore.theme.LocalAiAvailable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButlerCenteredTopAppBar(
    title: String,
    onBack: (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isAiAvailable = LocalAiAvailable.current
    val onAiClick = LocalAiAction.current

    CenterAlignedTopAppBar(
        title = { Text(title, modifier = Modifier.testTag("TopBarTitle")) },
        navigationIcon = {
            if (navigationIcon != null) {
                navigationIcon()
            } else if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = {
            actions()
            if (isAiAvailable) {
                FilledTonalIconButton(
                    onClick = onAiClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Assistant",
                    )
                }
            }
        },
        modifier = modifier,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(),
    )
}
