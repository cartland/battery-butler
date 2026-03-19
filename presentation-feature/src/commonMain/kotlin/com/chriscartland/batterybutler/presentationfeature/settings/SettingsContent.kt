package com.chriscartland.batterybutler.presentationfeature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_sign_out
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_engine_cloud
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_engine_noop
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_engine_on_device
import com.chriscartland.batterybutler.composeresources.generated.resources.ai_engine_title
import com.chriscartland.batterybutler.composeresources.generated.resources.network_mode_grpc_aws
import com.chriscartland.batterybutler.composeresources.generated.resources.network_mode_grpc_dev
import com.chriscartland.batterybutler.composeresources.generated.resources.network_mode_grpc_local
import com.chriscartland.batterybutler.composeresources.generated.resources.network_mode_mock
import com.chriscartland.batterybutler.composeresources.generated.resources.network_mode_none
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_app_version
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_check_updates_description
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_check_updates_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_copied_to_clipboard
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_export_data_description
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_export_data_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_title
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.NetworkMode
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.ExpandableSelectionControl
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    networkMode: NetworkMode,
    availableNetworkModes: List<NetworkMode>,
    onNetworkModeSelected: (NetworkMode) -> Unit,
    aiEngineType: AiEngineType,
    availableAiEngines: List<AiEngineType>,
    onAiEngineSelected: (AiEngineType) -> Unit,
    onExportData: () -> Unit,
    onBack: () -> Unit,
    appVersion: AppVersion,
    currentUser: User?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpandedNetworkModes: Boolean = false,
) {
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = composeStringResource(Res.string.settings_copied_to_clipboard)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.settings_title),
                onBack = onBack,
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(Padding.standard),
            verticalArrangement = Arrangement.spacedBy(Padding.standard),
        ) {
            // Account Card
            if (currentUser != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Padding.standard),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentUser.displayName ?: "Signed In",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                currentUser.email?.let { email ->
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onSignOut,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp),
                            )
                            Text(composeStringResource(Res.string.action_sign_out))
                        }
                    }
                }
            }

            // Network Mode Card
            ExpandableSelectionControl(
                title = "Network Mode",
                currentSelection = networkMode,
                options = availableNetworkModes,
                onOptionSelected = onNetworkModeSelected,
                leadingIcon = Icons.Default.Wifi,
                initiallyExpanded = initiallyExpandedNetworkModes,
                optionLabel = { mode ->
                    when (mode) {
                        is NetworkMode.None -> composeStringResource(Res.string.network_mode_none)

                        is NetworkMode.Mock -> composeStringResource(Res.string.network_mode_mock)

                        is NetworkMode.GrpcLocal -> composeStringResource(
                            Res.string.network_mode_grpc_local,
                        )

                        is NetworkMode.GrpcAws -> composeStringResource(
                            Res.string.network_mode_grpc_aws,
                        )

                        is NetworkMode.GrpcDev -> composeStringResource(
                            Res.string.network_mode_grpc_dev,
                        )
                    }
                },
            )

            // AI Engine Card
            ExpandableSelectionControl(
                title = composeStringResource(Res.string.ai_engine_title),
                currentSelection = aiEngineType,
                options = availableAiEngines,
                onOptionSelected = onAiEngineSelected,
                leadingIcon = Icons.Default.AutoAwesome,
                optionLabel = { mode ->
                    when (mode) {
                        AiEngineType.Cloud -> composeStringResource(Res.string.ai_engine_cloud)
                        AiEngineType.OnDevice -> composeStringResource(Res.string.ai_engine_on_device)
                        AiEngineType.NoOp -> composeStringResource(Res.string.ai_engine_noop)
                    }
                },
            )

            // Export Data Card
            Card(
                onClick = onExportData,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Padding.standard),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Padding.standard),
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = composeStringResource(Res.string.settings_export_data_title),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = composeStringResource(Res.string.settings_export_data_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = composeStringResource(Res.string.settings_export_data_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // Check for Updates Card
            Card(
                onClick = {
                    uriHandler.openUri(
                        "https://play.google.com/store/apps/details?id=com.chriscartland.batterybutler",
                    )
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Padding.standard),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Padding.standard),
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = composeStringResource(Res.string.settings_check_updates_title),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = composeStringResource(Res.string.settings_check_updates_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = composeStringResource(Res.string.settings_check_updates_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            // App Version Card
            val versionText = when (appVersion) {
                is AppVersion.Android -> "${appVersion.versionName}-${appVersion.versionCode}"
                is AppVersion.Ios -> "${appVersion.versionName}-${appVersion.buildNumber}"
                is AppVersion.Desktop -> appVersion.versionName
                is AppVersion.Unavailable -> "Unavailable"
            }
            Card(
                onClick = {
                    clipboardManager.setText(AnnotatedString(versionText))
                    scope.launch {
                        snackbarHostState.showSnackbar(copiedMessage)
                    }
                },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Padding.standard),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Padding.standard),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = composeStringResource(Res.string.settings_app_version),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = composeStringResource(Res.string.settings_app_version),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = versionText,
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
            networkMode = NetworkMode.Mock,
            availableNetworkModes = listOf(NetworkMode.Mock),
            onNetworkModeSelected = {},
            aiEngineType = AiEngineType.Cloud,
            availableAiEngines = AiEngineType.entries,
            onAiEngineSelected = {},
            onExportData = {},
            onBack = {},
            appVersion = AppVersion.Android("1.0.0", 123),
            currentUser = User(
                id = "preview-user",
                email = "user@example.com",
                displayName = "Jane Doe",
                photoUrl = null,
            ),
            onSignOut = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsContentAllNetworkModesPreview() {
    BatteryButlerTheme {
        SettingsContent(
            networkMode = NetworkMode.GrpcLocal("http://10.0.2.2:50051"),
            availableNetworkModes = listOf(
                NetworkMode.None,
                NetworkMode.Mock,
                NetworkMode.GrpcLocal("http://10.0.2.2:50051"),
                NetworkMode.GrpcDev("http://example.com:80"),
                NetworkMode.GrpcAws("http://example.com:80"),
            ),
            onNetworkModeSelected = {},
            aiEngineType = AiEngineType.Cloud,
            availableAiEngines = AiEngineType.entries,
            onAiEngineSelected = {},
            onExportData = {},
            onBack = {},
            appVersion = AppVersion.Android("1.0.0", 123),
            currentUser = null,
            onSignOut = {},
            initiallyExpandedNetworkModes = true,
        )
    }
}
