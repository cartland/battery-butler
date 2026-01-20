package com.chriscartland.batterybutler.presentationcore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hexagon
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PedalBike
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Propane
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Square
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.chriscartland.batterybutler.domain.model.DeviceIcons
import com.chriscartland.batterybutler.presentationcore.components.ButlerCenteredTopAppBar
import com.chriscartland.batterybutler.presentationcore.theme.BatteryButlerTheme

object DeviceIconMapper {
    val AvailableIcons = DeviceIcons.AvailableIcons

    fun getIcon(iconName: String?): ImageVector =
        when (iconName) {
            // Shapes
            "star" -> Icons.Default.Star
            "circle" -> Icons.Default.Circle
            "square" -> Icons.Default.Square
            "favorite" -> Icons.Default.Favorite
            "diamond" -> Icons.Default.Diamond
            "hexagon" -> Icons.Default.Hexagon
            // Electronics
            "smartphone" -> Icons.Default.Smartphone
            "tablet" -> Icons.Default.TabletMac
            "laptop" -> Icons.Default.Laptop
            "watch" -> Icons.Default.Watch
            "headphones" -> Icons.Default.Headphones
            "camera" -> Icons.Default.CameraAlt
            "speaker" -> Icons.Default.Speaker
            "videogame_asset" -> Icons.Default.VideogameAsset
            "game_controller" -> Icons.Default.SportsEsports
            "tv" -> Icons.Default.Tv
            "router" -> Icons.Default.Router
            "power" -> Icons.Default.Power
            "smart_button" -> Icons.Default.SmartButton
            "settings_remote" -> Icons.Default.SettingsRemote
            "mouse" -> Icons.Default.Mouse
            "keyboard" -> Icons.Default.Keyboard
            // Home
            "lightbulb" -> Icons.Default.Lightbulb
            "detector_smoke" -> Icons.Default.Propane
            "thermostat" -> Icons.Default.Thermostat
            "sensors" -> Icons.Default.Sensors
            "lock" -> Icons.Default.Lock
            "garage_home" -> Icons.Default.Garage
            // Tools/Utility
            "flashlight_on" -> Icons.Default.Highlight
            "drill" -> Icons.Default.Build
            "brush" -> Icons.Default.Brush
            "scale" -> Icons.Default.Scale
            "straighten" -> Icons.Default.Straighten
            "water_drop" -> Icons.Default.WaterDrop
            // Other
            "car" -> Icons.Default.DirectionsCar
            "bike" -> Icons.Default.PedalBike
            "schedule" -> Icons.Default.Schedule
            "location_on" -> Icons.Default.LocationOn
            "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            "toys" -> Icons.Default.Toys
            else -> Icons.Default.DevicesOther
        }
}

@Preview(showBackground = true)
@Composable
fun DeviceIconsPreview() {
    BatteryButlerTheme {
        Scaffold(
            topBar = { ButlerCenteredTopAppBar(title = "Device Icons", onBack = {}) },
        ) { innerPadding ->
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 80.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
            ) {
                items(DeviceIconMapper.AvailableIcons) { iconName ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = DeviceIconMapper.getIcon(iconName),
                                contentDescription = iconName,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                        Text(
                            text = iconName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
