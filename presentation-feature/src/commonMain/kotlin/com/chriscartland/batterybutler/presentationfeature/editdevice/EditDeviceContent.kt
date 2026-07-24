package com.chriscartland.batterybutler.presentationfeature.editdevice

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.composeresources.composeStringResource
import com.chriscartland.batterybutler.composeresources.generated.resources.Res
import com.chriscartland.batterybutler.composeresources.generated.resources.action_add_new_device_type
import com.chriscartland.batterybutler.composeresources.generated.resources.action_cancel
import com.chriscartland.batterybutler.composeresources.generated.resources.action_change_photo
import com.chriscartland.batterybutler.composeresources.generated.resources.action_choose_photo
import com.chriscartland.batterybutler.composeresources.generated.resources.action_delete
import com.chriscartland.batterybutler.composeresources.generated.resources.action_delete_device
import com.chriscartland.batterybutler.composeresources.generated.resources.action_remove_photo
import com.chriscartland.batterybutler.composeresources.generated.resources.action_save_bold
import com.chriscartland.batterybutler.composeresources.generated.resources.content_desc_device_photo
import com.chriscartland.batterybutler.composeresources.generated.resources.device_image_error_device_not_found
import com.chriscartland.batterybutler.composeresources.generated.resources.device_image_error_invalid_image
import com.chriscartland.batterybutler.composeresources.generated.resources.device_image_error_network
import com.chriscartland.batterybutler.composeresources.generated.resources.device_image_error_too_large
import com.chriscartland.batterybutler.composeresources.generated.resources.device_image_updated
import com.chriscartland.batterybutler.composeresources.generated.resources.dialog_delete_device_text
import com.chriscartland.batterybutler.composeresources.generated.resources.dialog_delete_device_title
import com.chriscartland.batterybutler.composeresources.generated.resources.edit_device_title
import com.chriscartland.batterybutler.composeresources.generated.resources.error_device_not_found
import com.chriscartland.batterybutler.composeresources.generated.resources.form_error_device_name_required
import com.chriscartland.batterybutler.composeresources.generated.resources.form_error_device_type_required
import com.chriscartland.batterybutler.composeresources.generated.resources.label_device_name
import com.chriscartland.batterybutler.composeresources.generated.resources.label_device_type
import com.chriscartland.batterybutler.composeresources.generated.resources.label_location
import com.chriscartland.batterybutler.composeresources.generated.resources.label_select_type
import com.chriscartland.batterybutler.domain.model.Device
import com.chriscartland.batterybutler.domain.model.DeviceImageBytes
import com.chriscartland.batterybutler.domain.model.DeviceImageError
import com.chriscartland.batterybutler.domain.model.DeviceInput
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.components.DeviceIconMapper
import com.chriscartland.batterybutler.presentationcore.components.rememberDeviceImageBitmap
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme
import com.chriscartland.batterybutler.presentationcore.theme.Padding
import com.chriscartland.batterybutler.presentationcore.util.DeviceImagePicker
import com.chriscartland.batterybutler.presentationcore.util.LocalDeviceImagePicker
import com.chriscartland.batterybutler.presentationcore.util.normalizeDeviceImage
import com.chriscartland.batterybutler.presentationmodel.editdevice.EditDeviceScreenState
import kotlinx.coroutines.delay
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceContent(
    uiState: EditDeviceScreenState,
    onSave: (DeviceInput) -> Unit,
    onDelete: () -> Unit,
    onAddDeviceTypeClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPhotoPicked: (bytes: ByteArray, contentType: String) -> Unit = { _, _ -> },
    onRemovePhoto: () -> Unit = {},
    onPhotoPickFailed: () -> Unit = {},
    onPhotoUpdatedShown: () -> Unit = {},
    photoError: DeviceImageError? = null,
    photoUploading: Boolean = false,
    photoUpdated: Boolean = false,
) {
    // Local state for form fields
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var selectedTypeId by rememberSaveable { mutableStateOf("") }
    var isInitialized by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var hasAttemptedSubmit by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val nameError = if (hasAttemptedSubmit && name.trim().isEmpty()) {
        composeStringResource(Res.string.form_error_device_name_required)
    } else {
        null
    }
    val typeError = if (hasAttemptedSubmit && selectedTypeId.isBlank()) {
        composeStringResource(Res.string.form_error_device_type_required)
    } else {
        null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = composeStringResource(Res.string.edit_device_title),
                onBack = onBack,
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(composeStringResource(Res.string.action_cancel))
                    }
                },
                actions = {
                    val trimmedName = name.trim()
                    val trimmedLocation = location.trim()
                    val isValid = trimmedName.isNotEmpty() && selectedTypeId.isNotBlank()
                    TextButton(
                        onClick = {
                            hasAttemptedSubmit = true
                            if (isValid) {
                                onSave(
                                    DeviceInput(
                                        name = trimmedName,
                                        location = trimmedLocation.takeIf { it.isNotEmpty() },
                                        typeId = selectedTypeId,
                                    ),
                                )
                            }
                        },
                    ) {
                        Text(composeStringResource(Res.string.action_save_bold), fontWeight = FontWeight.Bold)
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                EditDeviceScreenState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                EditDeviceScreenState.NotFound -> {
                    Text(composeStringResource(Res.string.error_device_not_found), modifier = Modifier.align(Alignment.Center))
                }

                is EditDeviceScreenState.Success -> {
                    // Initialize fields once
                    LaunchedEffect(state) {
                        if (!isInitialized) {
                            name = state.device.name
                            location = state.device.location ?: ""
                            selectedTypeId = state.device.typeId
                            isInitialized = true
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(Padding.standard),
                    ) {
                        if (state.imagesSupported) {
                            DevicePhotoSection(
                                imageEtag = state.device.imageEtag,
                                imageBytes = state.imageBytes,
                                photoError = photoError,
                                photoUploading = photoUploading,
                                photoUpdated = photoUpdated,
                                onPhotoPicked = onPhotoPicked,
                                onRemovePhoto = onRemovePhoto,
                                onPhotoPickFailed = onPhotoPickFailed,
                                onPhotoUpdatedShown = onPhotoUpdatedShown,
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Name Input
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(composeStringResource(Res.string.label_device_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = nameError != null,
                            supportingText = nameError?.let { msg -> { Text(msg) } },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Next) },
                            ),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = location,
                            onValueChange = { location = it },
                            label = { Text(composeStringResource(Res.string.label_location)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() },
                            ),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Type Dropdown
                        val selectedType = state.deviceTypes.find { it.id == selectedTypeId }
                        var expanded by remember { mutableStateOf(false) }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                        enabled = true,
                                    ).fillMaxWidth(),
                                readOnly = true,
                                value = selectedType?.name ?: composeStringResource(Res.string.label_select_type),
                                onValueChange = {},
                                label = { Text(composeStringResource(Res.string.label_device_type)) },
                                leadingIcon = if (selectedType != null) {
                                    {
                                        Icon(
                                            imageVector = DeviceIconMapper.getIcon(
                                                selectedType.defaultIcon,
                                            ),
                                            contentDescription = null,
                                        )
                                    }
                                } else {
                                    null
                                },
                                isError = typeError != null,
                                supportingText = typeError?.let { msg -> { Text(msg) } },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                            ) {
                                state.deviceTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.name) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = DeviceIconMapper.getIcon(
                                                    type.defaultIcon,
                                                ),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            selectedTypeId = type.id
                                            expanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text(composeStringResource(Res.string.action_add_new_device_type), fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        onAddDeviceTypeClick()
                                        expanded = false
                                    },
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Delete Button
                        Button(
                            onClick = {
                                showDeleteDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text(composeStringResource(Res.string.action_delete_device), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(composeStringResource(Res.string.dialog_delete_device_title)) },
                text = { Text(composeStringResource(Res.string.dialog_delete_device_text)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(composeStringResource(Res.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(composeStringResource(Res.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun DevicePhotoSection(
    imageEtag: String?,
    imageBytes: DeviceImageBytes?,
    photoError: DeviceImageError?,
    photoUploading: Boolean,
    photoUpdated: Boolean,
    onPhotoPicked: (bytes: ByteArray, contentType: String) -> Unit,
    onRemovePhoto: () -> Unit,
    onPhotoPickFailed: () -> Unit,
    onPhotoUpdatedShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = LocalDeviceImagePicker.current
    val imageBitmap = rememberDeviceImageBitmap(imageEtag, imageBytes)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = composeStringResource(Res.string.content_desc_device_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = composeStringResource(Res.string.content_desc_device_photo),
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (photoUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                enabled = !photoUploading,
                onClick = {
                    picker.pickImage { pickedBytes ->
                        if (pickedBytes != null) {
                            val normalized = normalizeDeviceImage(pickedBytes)
                            if (normalized != null) {
                                onPhotoPicked(normalized.bytes, normalized.contentType)
                            } else {
                                onPhotoPickFailed()
                            }
                        }
                    }
                },
            ) {
                Text(
                    composeStringResource(
                        if (imageBitmap != null) Res.string.action_change_photo else Res.string.action_choose_photo,
                    ),
                )
            }
            if (imageBitmap != null) {
                TextButton(enabled = !photoUploading, onClick = onRemovePhoto) {
                    Text(composeStringResource(Res.string.action_remove_photo))
                }
            }
        }

        if (photoError != null) {
            Text(
                text = getPhotoErrorText(photoError),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Transient "Photo updated" confirmation. Needed because a same-photo re-upload leaves the
        // avatar visually identical -- without this cue the user gets no signal it worked. Auto-clears
        // after a short delay so it doesn't linger.
        if (photoUpdated) {
            Text(
                text = composeStringResource(Res.string.device_image_updated),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        // rememberUpdatedState so the auto-clear effect restarts only on photoUpdated changes, while
        // still invoking the latest callback instance.
        val currentOnPhotoUpdatedShown by rememberUpdatedState(onPhotoUpdatedShown)
        LaunchedEffect(photoUpdated) {
            if (photoUpdated) {
                delay(PHOTO_UPDATED_CUE_DURATION_MS)
                currentOnPhotoUpdatedShown()
            }
        }
    }
}

/** How long the transient "Photo updated" confirmation stays visible before auto-clearing. */
private const val PHOTO_UPDATED_CUE_DURATION_MS = 2500L

@Composable
private fun getPhotoErrorText(error: DeviceImageError): String =
    when (error) {
        is DeviceImageError.InvalidImage -> composeStringResource(Res.string.device_image_error_invalid_image)
        is DeviceImageError.DeviceNotFound -> composeStringResource(Res.string.device_image_error_device_not_found)
        is DeviceImageError.TooLarge -> composeStringResource(Res.string.device_image_error_too_large)
        is DeviceImageError.NetworkError -> composeStringResource(Res.string.device_image_error_network)
    }

@Preview(showBackground = true)
@Composable
fun EditDeviceLoadingPreview() {
    BatteryButlerTheme {
        EditDeviceContent(
            uiState = EditDeviceScreenState.Loading,
            onSave = {},
            onDelete = {},
            onAddDeviceTypeClick = {},
            onBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EditDeviceNotFoundPreview() {
    BatteryButlerTheme {
        EditDeviceContent(
            uiState = EditDeviceScreenState.NotFound,
            onSave = {},
            onDelete = {},
            onAddDeviceTypeClick = {},
            onBack = {},
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun EditDeviceContentPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        EditDeviceContent(
            uiState = EditDeviceScreenState.Success(
                device = device,
                deviceTypes = listOf(type),
            ),
            onSave = {},
            onDelete = {},
            onAddDeviceTypeClick = {},
            onBack = {},
        )
    }
}

@OptIn(ExperimentalTime::class)
@Preview(showBackground = true)
@Composable
fun EditDeviceContentPhotoPreview() {
    BatteryButlerTheme {
        val now = Instant.parse("2026-01-18T17:00:00Z")
        val type = DeviceType("type1", "Smoke Alarm", "detector_smoke")
        val device = Device("dev1", "Kitchen Smoke", "type1", now, now, "Kitchen")
        CompositionLocalProvider(LocalDeviceImagePicker provides DeviceImagePicker { onResult -> onResult(null) }) {
            EditDeviceContent(
                uiState = EditDeviceScreenState.Success(
                    device = device,
                    deviceTypes = listOf(type),
                    imagesSupported = true,
                ),
                onSave = {},
                onDelete = {},
                onAddDeviceTypeClick = {},
                onBack = {},
                photoError = DeviceImageError.TooLarge(),
            )
        }
    }
}
