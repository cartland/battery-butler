package com.chriscartland.blanket.feature.devicetypes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chriscartland.blanket.ui.components.DeviceIconMapper

@Composable
fun DeviceTypeListScreen(
    viewModel: DeviceTypeListViewModel,
    onEditType: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
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
