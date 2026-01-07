package com.chriscartland.batterybutler.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.chriscartland.batterybutler.ui.theme.LocalAiAction
import com.chriscartland.batterybutler.ui.theme.LocalAiAvailable

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
        title = { Text(title) },
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
                IconButton(onClick = onAiClick) {
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
