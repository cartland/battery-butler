package com.chriscartland.batterybutler.presentationfeature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
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
import com.chriscartland.batterybutler.composeresources.generated.resources.data_mode_grpc_aws
import com.chriscartland.batterybutler.composeresources.generated.resources.data_mode_grpc_dev
import com.chriscartland.batterybutler.composeresources.generated.resources.data_mode_grpc_local
import com.chriscartland.batterybutler.composeresources.generated.resources.data_mode_labs_prod
import com.chriscartland.batterybutler.composeresources.generated.resources.data_mode_labs_staging
import com.chriscartland.batterybutler.composeresources.generated.resources.data_mode_mock
import com.chriscartland.batterybutler.composeresources.generated.resources.data_mode_none
import com.chriscartland.batterybutler.composeresources.generated.resources.data_mode_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_advanced_copy_labs_token
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_advanced_copy_labs_token_description
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_advanced_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_app_version
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_app_version_unavailable
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_check_updates_description
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_check_updates_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_copied_to_clipboard
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_file
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_legacy_found
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_restore
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_restore_complete
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_restore_destructive
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_restore_failed
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_restore_unavailable
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_restoring
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_database_version
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_export_data_description
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_export_data_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_import_data_description
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_import_data_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_import_failed_message
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_import_success_message
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_description
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_session_expired
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_sign_in
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_sign_out
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_signed_in
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_signing_in
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_labs_token_copied
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_section_about
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_section_account
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_section_data
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_title
import com.chriscartland.batterybutler.composeresources.generated.resources.settings_user_signed_in
import com.chriscartland.batterybutler.domain.model.AppVersion
import com.chriscartland.batterybutler.domain.model.AuthState
import com.chriscartland.batterybutler.domain.model.DataMode
import com.chriscartland.batterybutler.domain.model.ImportResult
import com.chriscartland.batterybutler.domain.model.LegacyDatabaseInfo
import com.chriscartland.batterybutler.domain.model.RestoreResult
import com.chriscartland.batterybutler.domain.model.SignedOutCause
import com.chriscartland.batterybutler.domain.model.User
import com.chriscartland.batterybutler.domain.model.ai.AiEngineType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.ExpandableSelectionControl
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationfeature.auth.isLabsSessionExpired
import com.chriscartland.batterybutler.presentationfeature.auth.labsAuthErrorText
import kotlinx.coroutines.launch

/** Test tags for [SettingsContent] — used by headless UI tests to pin section order. */
internal object SettingsTestTags {
    const val SECTION_ACCOUNT = "settings_section_account"
    const val SECTION_DATA = "settings_section_data"
    const val SECTION_ABOUT = "settings_section_about"
    const val SECTION_ADVANCED = "settings_section_advanced"
    const val DATA_MODE = "settings_data_mode"
    const val AI_ENGINE = "settings_ai_engine"
    const val COPY_LABS_TOKEN = "settings_copy_labs_token"
}

/**
 * The Settings tab, organized user-relevant → developer:
 *
 * 1. **Account** — the sign-in surface(s). The Labs card whenever a Labs data mode is
 *    selected; the own-backend account card whenever that user is signed in. Hidden
 *    entirely when there is no account to show.
 * 2. **Data** — Export and Import (backup/restore are one mental unit).
 * 3. **About** — Check for updates + app version.
 * 4. **Advanced** — environment and developer controls: the Data Mode picker (an
 *    environment switch, not an everyday setting), the Labs ID token copy, the
 *    Database card, and the AI engine picker (implementation config for the
 *    assistant, not a feature toggle).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    dataMode: DataMode,
    availableDataModes: List<DataMode>,
    onDataModeSelected: (DataMode) -> Unit,
    aiEngineType: AiEngineType,
    availableAiEngines: List<AiEngineType>,
    onAiEngineSelected: (AiEngineType) -> Unit,
    onExportData: () -> Unit,
    onImportData: () -> Unit,
    importResult: ImportResult?,
    importError: String?,
    importInProgress: Boolean,
    onImportResultConsumed: () -> Unit,
    onBack: () -> Unit,
    appVersion: AppVersion,
    currentUser: User?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    initiallyExpandedDataModes: Boolean = false,
    currentDatabaseFileName: String = "",
    legacyDatabaseInfo: LegacyDatabaseInfo? = null,
    onRestoreLegacyDatabase: () -> Unit = {},
    restoreInProgress: Boolean = false,
    restoreComplete: Boolean = false,
    restoreResult: RestoreResult? = null,
    onRestoreCompleteAcknowledged: () -> Unit = {},
    isLabsMode: Boolean = false,
    labsAuthState: AuthState = AuthState.Unauthenticated(),
    onSignInToLabs: () -> Unit = {},
    onSignOutLabs: () -> Unit = {},
    onCopyLabsIdToken: () -> Unit = {},
    labsIdTokenToCopy: String? = null,
    onLabsIdTokenCopied: () -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current

    // LocalClipboardManager remains in use because CMP 1.10's LocalClipboard requires an
    // expect/actual ClipEntry factory (the constructor takes a platform-specific native object).
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = composeStringResource(Res.string.settings_copied_to_clipboard)
    val restoreCompleteMessage = composeStringResource(Res.string.settings_database_restore_complete)
    val restoreDestructiveMessage = when (restoreResult) {
        is RestoreResult.DestructiveFallback -> composeStringResource(
            Res.string.settings_database_restore_destructive,
            restoreResult.fromVersion.toString(),
        )

        else -> ""
    }
    val restoreFailedMessage = when (restoreResult) {
        is RestoreResult.Failure -> composeStringResource(
            Res.string.settings_database_restore_failed,
            restoreResult.errorMessage,
        )

        else -> ""
    }
    val restoreUnavailableMessage = when (restoreResult) {
        is RestoreResult.LegacyFileUnavailable -> composeStringResource(
            Res.string.settings_database_restore_unavailable,
            restoreResult.reason,
        )

        else -> ""
    }
    val currentOnRestoreCompleteAcknowledged = rememberUpdatedState(onRestoreCompleteAcknowledged)

    val currentOnImportResultConsumed = rememberUpdatedState(onImportResultConsumed)

    val currentOnLabsIdTokenCopied = rememberUpdatedState(onLabsIdTokenCopied)
    val tokenCopiedMessage = composeStringResource(Res.string.settings_labs_token_copied)

    // Drive snackbar from restoreResult (richer signal). The boolean restoreComplete
    // remains in the API for callers that only want the success-or-fallback path,
    // but the snackbar messaging differentiates Success / DestructiveFallback /
    // Failure / LegacyFileUnavailable.
    LaunchedEffect(restoreResult) {
        val message = when (restoreResult) {
            is RestoreResult.Success -> restoreCompleteMessage
            is RestoreResult.DestructiveFallback -> restoreDestructiveMessage
            is RestoreResult.Failure -> restoreFailedMessage
            is RestoreResult.LegacyFileUnavailable -> restoreUnavailableMessage
            null -> null
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            currentOnRestoreCompleteAcknowledged.value()
        }
    }

    // The actual clipboard write happens in SettingsScreen (it owns platform composition
    // locals); this just confirms it happened and clears the pending-copy signal, same
    // separation of concerns as the file-based export/import flows.
    LaunchedEffect(labsIdTokenToCopy) {
        if (labsIdTokenToCopy != null) {
            snackbarHostState.showSnackbar(tokenCopiedMessage)
            currentOnLabsIdTokenCopied.value()
        }
    }

    val importSuccessMessage = composeStringResource(
        Res.string.settings_import_success_message,
        importResult?.devicesImported?.toString().orEmpty(),
        importResult?.deviceTypesImported?.toString().orEmpty(),
        importResult?.eventsImported?.toString().orEmpty(),
    )
    val importFailedMessage = composeStringResource(
        Res.string.settings_import_failed_message,
        importError.orEmpty(),
    )

    LaunchedEffect(importResult) {
        if (importResult != null) {
            snackbarHostState.showSnackbar(importSuccessMessage)
            currentOnImportResultConsumed.value()
        }
    }

    LaunchedEffect(importError) {
        if (importError != null) {
            snackbarHostState.showSnackbar(importFailedMessage)
            currentOnImportResultConsumed.value()
        }
    }

    val appVersionText = when (appVersion) {
        is AppVersion.Android -> "${appVersion.versionName}-${appVersion.versionCode}"
        is AppVersion.Ios -> "${appVersion.versionName}-${appVersion.buildNumber}"
        is AppVersion.Desktop -> appVersion.versionName
        is AppVersion.Unavailable -> composeStringResource(Res.string.settings_app_version_unavailable)
    }

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
                .verticalScroll(rememberScrollState())
                .padding(Padding.standard),
            verticalArrangement = Arrangement.spacedBy(Padding.standard),
        ) {
            // Account — the sign-in surface(s), first because it's what users touch most.
            if (isLabsMode || currentUser != null) {
                SettingsSectionHeader(
                    composeStringResource(Res.string.settings_section_account),
                    modifier = Modifier.testTag(SettingsTestTags.SECTION_ACCOUNT),
                )
            }

            // Labs Sign-In Card — only when a Labs (REST) data mode is selected. The Labs
            // backend needs its own Google sign-in (separate account from the gRPC backend).
            if (isLabsMode) {
                LabsAccountCard(
                    labsAuthState = labsAuthState,
                    onSignInToLabs = onSignInToLabs,
                    onSignOutLabs = onSignOutLabs,
                )
            }

            // Own-backend account card — only when that user is signed in.
            if (currentUser != null) {
                AppAccountCard(
                    currentUser = currentUser,
                    onSignOut = onSignOut,
                )
            }

            // Data — backup and restore are one mental unit.
            SettingsSectionHeader(
                composeStringResource(Res.string.settings_section_data),
                modifier = Modifier.testTag(SettingsTestTags.SECTION_DATA),
            )

            SettingsActionCard(
                icon = Icons.Default.Download,
                title = composeStringResource(Res.string.settings_export_data_title),
                description = composeStringResource(Res.string.settings_export_data_description),
                onClick = onExportData,
            )

            SettingsActionCard(
                icon = Icons.Default.Upload,
                title = composeStringResource(Res.string.settings_import_data_title),
                description = composeStringResource(Res.string.settings_import_data_description),
                onClick = onImportData,
                enabled = !importInProgress,
            )

            // About
            SettingsSectionHeader(
                composeStringResource(Res.string.settings_section_about),
                modifier = Modifier.testTag(SettingsTestTags.SECTION_ABOUT),
            )

            SettingsActionCard(
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                title = composeStringResource(Res.string.settings_check_updates_title),
                description = composeStringResource(Res.string.settings_check_updates_description),
                onClick = {
                    uriHandler.openUri(
                        "https://play.google.com/store/apps/details?id=com.chriscartland.batterybutler",
                    )
                },
            )

            SettingsActionCard(
                icon = Icons.Default.Info,
                title = composeStringResource(Res.string.settings_app_version),
                description = appVersionText,
                onClick = {
                    clipboardManager.setText(AnnotatedString(appVersionText))
                    scope.launch {
                        snackbarHostState.showSnackbar(copiedMessage)
                    }
                },
            )

            // Advanced — environment and developer controls, last on the page.
            SettingsSectionHeader(
                composeStringResource(Res.string.settings_advanced_title),
                modifier = Modifier.testTag(SettingsTestTags.SECTION_ADVANCED),
            )

            // Data Mode picker — an environment switch (Production / Staging / Mock),
            // not an everyday setting.
            ExpandableSelectionControl(
                title = composeStringResource(Res.string.data_mode_title),
                currentSelection = dataMode,
                options = availableDataModes,
                onOptionSelected = onDataModeSelected,
                leadingIcon = Icons.Default.Wifi,
                initiallyExpanded = initiallyExpandedDataModes,
                modifier = Modifier.testTag(SettingsTestTags.DATA_MODE),
                optionLabel = { mode ->
                    when (mode) {
                        is DataMode.None -> composeStringResource(Res.string.data_mode_none)

                        is DataMode.Mock -> composeStringResource(Res.string.data_mode_mock)

                        is DataMode.GrpcLocal -> composeStringResource(
                            Res.string.data_mode_grpc_local,
                        )

                        is DataMode.GrpcAws -> composeStringResource(
                            Res.string.data_mode_grpc_aws,
                        )

                        is DataMode.GrpcDev -> composeStringResource(
                            Res.string.data_mode_grpc_dev,
                        )

                        is DataMode.LabsStaging -> composeStringResource(
                            Res.string.data_mode_labs_staging,
                        )

                        is DataMode.LabsProd -> composeStringResource(
                            Res.string.data_mode_labs_prod,
                        )
                    }
                },
            )

            // Copy Labs ID Token — only meaningful once actually signed in to Labs, since
            // that's the only time a token exists to copy.
            if (isLabsMode && labsAuthState is AuthState.Authenticated) {
                CopyLabsIdTokenCard(
                    onCopyLabsIdToken = onCopyLabsIdToken,
                    modifier = Modifier.testTag(SettingsTestTags.COPY_LABS_TOKEN),
                )
            }

            // Database card — the file shown depends on the selected data mode.
            if (currentDatabaseFileName.isNotEmpty()) {
                DatabaseCard(
                    currentDatabaseFileName = currentDatabaseFileName,
                    appVersionText = appVersionText,
                    legacyDatabaseInfo = legacyDatabaseInfo,
                    onRestoreLegacyDatabase = onRestoreLegacyDatabase,
                    restoreInProgress = restoreInProgress,
                )
            }

            // AI Engine picker — implementation config for the assistant
            // (Cloud / On-Device / Disabled), not a feature toggle.
            ExpandableSelectionControl(
                title = composeStringResource(Res.string.ai_engine_title),
                currentSelection = aiEngineType,
                options = availableAiEngines,
                onOptionSelected = onAiEngineSelected,
                leadingIcon = Icons.Default.AutoAwesome,
                modifier = Modifier.testTag(SettingsTestTags.AI_ENGINE),
                optionLabel = { mode ->
                    when (mode) {
                        AiEngineType.Cloud -> composeStringResource(Res.string.ai_engine_cloud)
                        AiEngineType.OnDevice -> composeStringResource(Res.string.ai_engine_on_device)
                        AiEngineType.NoOp -> composeStringResource(Res.string.ai_engine_noop)
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = Padding.small, top = Padding.small),
    )
}

/**
 * The shared icon + title + description row card used by simple Settings actions
 * (Export, Import, Check for updates, App version).
 */
@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Padding.standard),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Padding.standard),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/** The Labs sign-in card: identity when signed in, sign in/out, session-expired copy. */
@Composable
private fun LabsAccountCard(
    labsAuthState: AuthState,
    onSignInToLabs: () -> Unit,
    onSignOutLabs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
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
                        text = composeStringResource(Res.string.settings_labs_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val labsUser = (labsAuthState as? AuthState.Authenticated)?.user
                    Text(
                        text = when {
                            labsUser != null -> {
                                labsUser.email ?: composeStringResource(Res.string.settings_labs_signed_in)
                            }

                            // Reactive session loss: name the reason instead of the
                            // generic sign-in pitch, so the signed-out card doesn't
                            // read like the user was never signed in.
                            isLabsSessionExpired(labsAuthState) -> {
                                composeStringResource(Res.string.settings_labs_session_expired)
                            }

                            else -> {
                                composeStringResource(Res.string.settings_labs_description)
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (labsAuthState) {
                is AuthState.Authenticated -> {
                    Button(
                        onClick = onSignOutLabs,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.padding(end = Padding.small),
                        )
                        Text(composeStringResource(Res.string.settings_labs_sign_out))
                    }
                }

                AuthState.Authenticating -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(composeStringResource(Res.string.settings_labs_signing_in))
                    }
                }

                AuthState.Unknown -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                    )
                }

                is AuthState.Unauthenticated, is AuthState.Failed -> {
                    Button(
                        onClick = onSignInToLabs,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(composeStringResource(Res.string.settings_labs_sign_in))
                    }
                }
            }
            if (labsAuthState is AuthState.Failed) {
                Spacer(modifier = Modifier.height(8.dp))
                // Labs-specific copy (labsAuthErrorText) rather than the raw
                // AuthError cause, which carried internal detail like
                // "signInWithIdp HTTP 500".
                Text(
                    text = composeStringResource(
                        labsAuthErrorText(labsAuthState.error).message,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** The own-backend (gRPC) account card: identity + sign out. */
@Composable
private fun AppAccountCard(
    currentUser: User,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
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
                        text = currentUser.displayName ?: composeStringResource(Res.string.settings_user_signed_in),
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
                    modifier = Modifier.padding(end = Padding.small),
                )
                Text(composeStringResource(Res.string.action_sign_out))
            }
        }
    }
}

/** The developer card for copying a live Labs ID token, with its handle-like-a-password warning. */
@Composable
private fun CopyLabsIdTokenCard(
    onCopyLabsIdToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Padding.standard),
        ) {
            Text(
                text = composeStringResource(Res.string.settings_advanced_copy_labs_token),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = composeStringResource(
                    Res.string.settings_advanced_copy_labs_token_description,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onCopyLabsIdToken,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(composeStringResource(Res.string.settings_advanced_copy_labs_token))
            }
        }
    }
}

/** The Database card: current file + version, and the legacy-restore flow when available. */
@Composable
private fun DatabaseCard(
    currentDatabaseFileName: String,
    appVersionText: String,
    legacyDatabaseInfo: LegacyDatabaseInfo?,
    onRestoreLegacyDatabase: () -> Unit,
    restoreInProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Padding.standard),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Padding.standard),
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = composeStringResource(Res.string.settings_database_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = composeStringResource(
                            Res.string.settings_database_file,
                            currentDatabaseFileName,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Text(
                        text = composeStringResource(
                            Res.string.settings_database_version,
                            appVersionText,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
            if (legacyDatabaseInfo?.exists == true) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = composeStringResource(
                        Res.string.settings_database_legacy_found,
                        legacyDatabaseInfo.legacyFileName,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRestoreLegacyDatabase,
                    enabled = !restoreInProgress,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (restoreInProgress) {
                        Text(
                            composeStringResource(Res.string.settings_database_restoring),
                        )
                    } else {
                        Text(
                            composeStringResource(Res.string.settings_database_restore),
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
            dataMode = DataMode.Mock,
            availableDataModes = listOf(DataMode.Mock),
            onDataModeSelected = {},
            aiEngineType = AiEngineType.Cloud,
            availableAiEngines = AiEngineType.entries,
            onAiEngineSelected = {},
            onExportData = {},
            onImportData = {},
            importResult = null,
            importError = null,
            importInProgress = false,
            onImportResultConsumed = {},
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
fun SettingsContentAllDataModesPreview() {
    BatteryButlerTheme {
        SettingsContent(
            dataMode = DataMode.GrpcLocal("http://10.0.2.2:50051"),
            availableDataModes = listOf(
                DataMode.None,
                DataMode.Mock,
                DataMode.GrpcLocal("http://10.0.2.2:50051"),
                DataMode.GrpcDev("http://example.com:80"),
                DataMode.GrpcAws("http://example.com:80"),
            ),
            onDataModeSelected = {},
            aiEngineType = AiEngineType.Cloud,
            availableAiEngines = AiEngineType.entries,
            onAiEngineSelected = {},
            onExportData = {},
            onImportData = {},
            importResult = null,
            importError = null,
            importInProgress = false,
            onImportResultConsumed = {},
            onBack = {},
            appVersion = AppVersion.Android("1.0.0", 123),
            currentUser = null,
            onSignOut = {},
            initiallyExpandedDataModes = true,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsContentLabsAuthUnknownPreview() {
    BatteryButlerTheme {
        SettingsContent(
            dataMode = DataMode.LabsStaging(null),
            availableDataModes = listOf(DataMode.LabsStaging(null)),
            onDataModeSelected = {},
            aiEngineType = AiEngineType.Cloud,
            availableAiEngines = AiEngineType.entries,
            onAiEngineSelected = {},
            onExportData = {},
            onImportData = {},
            importResult = null,
            importError = null,
            importInProgress = false,
            onImportResultConsumed = {},
            onBack = {},
            appVersion = AppVersion.Android("1.0.0", 123),
            currentUser = null,
            onSignOut = {},
            isLabsMode = true,
            labsAuthState = AuthState.Unknown,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsContentLabsSessionExpiredPreview() {
    BatteryButlerTheme {
        SettingsContent(
            dataMode = DataMode.LabsStaging(null),
            availableDataModes = listOf(DataMode.LabsStaging(null)),
            onDataModeSelected = {},
            aiEngineType = AiEngineType.Cloud,
            availableAiEngines = AiEngineType.entries,
            onAiEngineSelected = {},
            onExportData = {},
            onImportData = {},
            importResult = null,
            importError = null,
            importInProgress = false,
            onImportResultConsumed = {},
            onBack = {},
            appVersion = AppVersion.Android("1.0.0", 123),
            currentUser = null,
            onSignOut = {},
            isLabsMode = true,
            labsAuthState = AuthState.Unauthenticated(cause = SignedOutCause.SESSION_EXPIRED),
        )
    }
}
