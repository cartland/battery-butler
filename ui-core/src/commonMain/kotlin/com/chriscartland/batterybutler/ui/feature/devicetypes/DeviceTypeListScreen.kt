package com.chriscartland.batterybutler.ui.feature.devicetypes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.DeviceType
import com.chriscartland.batterybutler.ui.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.ui.components.DeviceIconMapper

sealed interface DeviceTypeListUiState {
    data object Loading : DeviceTypeListUiState

    data class Success(
        val deviceTypes: List<DeviceType>,
    ) : DeviceTypeListUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceTypeListScreen(
    state: DeviceTypeListUiState,
    onEditType: (String) -> Unit,
    onAddType: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            ButlerCenteredTopAppBar(
                title = "Device Types",
                onBack = onBack,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddType) {
                Icon(Icons.Default.Add, contentDescription = "Add Type")
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (state) {
                DeviceTypeListUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is DeviceTypeListUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                    ) {
                        items(state.deviceTypes) { type ->
                            ListItem(
                                headlineContent = { Text(type.name, fontWeight = FontWeight.Medium) },
                                supportingContent = { Text("${type.batteryQuantity} x ${type.batteryType}") },
                                leadingContent = {
                                    Icon(
                                        imageVector = DeviceIconMapper.getIcon(type.defaultIcon),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                },
                                modifier = Modifier.clickable { onEditType(type.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
