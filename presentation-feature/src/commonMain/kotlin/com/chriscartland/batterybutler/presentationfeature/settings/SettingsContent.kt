package com.chriscartland.batterybutler.presentationfeature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.network_mode_grpc_local
import com.chriscartland.batterybutler.composeresources.generated.resources.network_mode_mock
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.ExpandableSelectionControl
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import org.jetbrains.compose.resources.stringResource as composeStringResource

@Composable
fun SettingsContent(
    networkMode: NetworkMode,
    onNetworkModeSelected: (NetworkMode) -> Unit,
    onExportData: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ButlerCenteredTopAppBar(
                title = "Settings",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Network Mode Card
            ExpandableSelectionControl(
                title = "Network Mode",
                currentSelection = networkMode,
                options = NetworkMode.entries,
                onOptionSelected = onNetworkModeSelected,
                optionLabel = { mode ->
                    when (mode) {
                        NetworkMode.MOCK -> composeStringResource(Res.string.network_mode_mock)
                        NetworkMode.GRPC_LOCAL -> composeStringResource(
                            Res.string.network_mode_grpc_local,
                        )
                    }
                },
            )

            // Export Data Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExportData() },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export Data",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = "Export Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Save all data as a .json file",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsContentPreview() {
    BatteryButlerTheme {
        SettingsContent(
            networkMode = NetworkMode.MOCK,
            onNetworkModeSelected = {},
            onExportData = {},
            onBack = {},
        )
    }
}
