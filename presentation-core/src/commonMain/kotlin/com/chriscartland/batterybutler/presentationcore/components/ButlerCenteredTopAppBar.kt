package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_back
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ButlerCenteredTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(title, modifier = Modifier.testTag("TopBarTitle")) },
        navigationIcon = {
            if (navigationIcon != null) {
                navigationIcon()
            } else if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = composeStringResource(Res.string.content_desc_back),
                    )
                }
            }
        },
        actions = {
            actions()
        },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(),
    )
}

@Preview(showBackground = true)
@Composable
fun ButlerCenteredTopAppBarPreview() {
    BatteryButlerTheme {
        ButlerCenteredTopAppBar(
            title = "Preview Title",
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ButlerCenteredTopAppBarWithActionsPreview() {
    BatteryButlerTheme {
        ButlerCenteredTopAppBar(
            title = "With Actions",
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Sample Action")
                }
            },
        )
    }
}
