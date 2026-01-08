package com.chriscartland.batterybutler.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DevicesOther
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
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.SmartButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object DeviceIconMapper {
    val AvailableIcons = listOf(
        // Shapes
        "star", "circle", "square", "favorite", "diamond", "hexagon",
        // Electronics
        "smartphone", "tablet", "laptop", "watch", "headphones", "camera", "speaker", "videogame_asset", "game_controller",
        "tv", "router", "power", "smart_button", "settings_remote", "mouse", "keyboard",
        // Home
        "lightbulb", "detector_smoke", "thermostat", "sensors", "lock", "garage_home",
        // Tools/Utility
        "flashlight_on", "drill", "brush", "scale", "straighten", "water_drop",
        // Other
        "car", "bike", "schedule", "location_on", "account_balance_wallet", "toys"
    )

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

    fun getContainerColor(iconName: String?): Color =
        when (iconName) {
            // Shapes
            "star" -> Color(0xFFFEF9C3) // yellow-100
            "circle", "square", "hexagon" -> Color(0xFFF3F4F6) // gray-100
            "favorite" -> Color(0xFFFEE2E2) // red-100
            "diamond" -> Color(0xFFE0F2FE) // sky-100
            // Electronics
            "smartphone", "tablet", "laptop", "watch", "headphones", "camera", "speaker", "router", "power" -> Color(0xFFE0F2FE) // sky-100
            "videogame_asset", "game_controller", "toys" -> Color(0xFFE0F2FE) // sky-100
            "tv", "mouse", "keyboard", "settings_remote" -> Color(0xFFF1F5F9) // slate-100
            "smart_button" -> Color(0xFFF3E8FF) // purple-100
            // Home
            "lightbulb" -> Color(0xFFFEF9C3) // yellow-100
            "detector_smoke" -> Color(0xFFFEE2E2) // red-100
            "thermostat" -> Color(0xFFCCFBF1) // teal-100
            "sensors" -> Color(0xFFFFEDD5) // orange-100
            "lock" -> Color(0xFFDBEAFE) // blue-100
            "garage_home" -> Color(0xFFF1F5F9) // slate-100
            // Tools/Utility
            "flashlight_on" -> Color(0xFFFEF9C3) // yellow-100
            "drill", "brush", "straighten", "scale" -> Color(0xFFF3F4F6) // gray-100
            "water_drop" -> Color(0xFFE0F2FE) // sky-100
            // Other
            "car", "bike" -> Color(0xFFDBEAFE) // blue-100
            "schedule" -> Color(0xFFEDE9FE) // violet-100
            "location_on" -> Color(0xFFDBEAFE) // blue-100
            "account_balance_wallet" -> Color(0xFFD1FAE5) // emerald-100
            else -> Color(0xFFF1F5F9)
        }

    fun getContentColor(iconName: String?): Color =
        when (iconName) {
            // Shapes
            "star" -> Color(0xFFCA8A04) // yellow-600
            "circle", "square", "hexagon" -> Color(0xFF4B5563) // gray-600
            "favorite" -> Color(0xFFDC2626) // red-600
            "diamond" -> Color(0xFF0284C7) // sky-600
            // Electronics
            "smartphone", "tablet", "laptop", "watch", "headphones", "camera", "speaker", "router", "power" -> Color(0xFF0284C7) // sky-600
            "videogame_asset", "game_controller", "toys" -> Color(0xFF0284C7) // sky-600
            "tv", "mouse", "keyboard", "settings_remote" -> Color(0xFF475569) // slate-600
            "smart_button" -> Color(0xFF9333EA) // purple-600
            // Home
            "lightbulb" -> Color(0xFFCA8A04) // yellow-600
            "detector_smoke" -> Color(0xFFDC2626) // red-600
            "thermostat" -> Color(0xFF0D9488) // teal-600
            "sensors" -> Color(0xFFEA580C) // orange-600
            "lock" -> Color(0xFF137FEC) // primary blue
            "garage_home" -> Color(0xFF475569) // slate-600
            // Tools/Utility
            "flashlight_on" -> Color(0xFFCA8A04) // yellow-600
            "drill", "brush", "straighten", "scale" -> Color(0xFF4B5563) // gray-600
            "water_drop" -> Color(0xFF0284C7) // sky-600
            // Other
            "car", "bike" -> Color(0xFF137FEC) // blue-600
            "schedule" -> Color(0xFF7C3AED) // violet-600
            "location_on" -> Color(0xFF137FEC) // blue-600
            "account_balance_wallet" -> Color(0xFF059669) // emerald-600
            else -> Color(0xFF475569)
        }
}
