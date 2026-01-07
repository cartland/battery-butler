package com.chriscartland.blanket.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Garage
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Propane
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Toys
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

object DeviceIconMapper {
    fun getIcon(iconName: String?): ImageVector =
        when (iconName) {
            "lock" -> Icons.Default.Lock
            "sensors" -> Icons.Default.Sensors
            "smart_button" -> Icons.Default.SmartButton
            "detector_smoke" -> Icons.Default.Propane // Closest match for now
            "thermostat" -> Icons.Default.Thermostat
            "garage_home" -> Icons.Default.Garage
            "videogame_asset" -> Icons.Default.Gamepad
            "tv" -> Icons.Default.Tv
            "toys" -> Icons.Default.Toys
            "mouse" -> Icons.Default.Mouse
            "keyboard" -> Icons.Default.Keyboard
            "flashlight_on" -> Icons.Default.Highlight // FlashlightOn might not be available in default set, use Highlight or similar
            "schedule" -> Icons.Default.Schedule
            "location_on" -> Icons.Default.LocationOn
            "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
            "straighten" -> Icons.Default.Straighten
            "water_drop" -> Icons.Default.WaterDrop
            else -> Icons.Default.DevicesOther
        }

    fun getContainerColor(iconName: String?): Color {
        // Approximate colors from design (Tailwind blue-100, orange-100, etc.)
        return when (iconName) {
            "lock" -> Color(0xFFDBEAFE) // blue-100
            "sensors" -> Color(0xFFFFEDD5) // orange-100
            "smart_button" -> Color(0xFFF3E8FF) // purple-100
            "detector_smoke" -> Color(0xFFFEE2E2) // red-100
            "thermostat" -> Color(0xFFCCFBF1) // teal-100
            "garage_home" -> Color(0xFFF1F5F9) // slate-100
            "videogame_asset" -> Color(0xFFE0E7FF) // indigo-100
            "tv" -> Color(0xFFE0F2FE) // sky-100
            "toys" -> Color(0xFFFCE7F3) // pink-100
            "mouse" -> Color(0xFFF3F4F6) // gray-100
            "keyboard" -> Color(0xFFF3F4F6) // gray-100
            "flashlight_on" -> Color(0xFFFEF9C3) // yellow-100
            "schedule" -> Color(0xFFEDE9FE) // violet-100
            "location_on" -> Color(0xFFDBEAFE) // blue-100
            "account_balance_wallet" -> Color(0xFFD1FAE5) // emerald-100
            "straighten" -> Color(0xFFF3F4F6) // gray-100
            "water_drop" -> Color(0xFFE0F2FE) // sky-100
            else -> Color(0xFFF1F5F9)
        }
    }

    fun getContentColor(iconName: String?): Color =
        when (iconName) {
            "lock" -> Color(0xFF137FEC) // primary blue
            "sensors" -> Color(0xFFEA580C) // orange-600
            "smart_button" -> Color(0xFF9333EA) // purple-600
            "detector_smoke" -> Color(0xFFDC2626) // red-600
            "thermostat" -> Color(0xFF0D9488) // teal-600
            "garage_home" -> Color(0xFF475569) // slate-600
            "videogame_asset" -> Color(0xFF4F46E5) // indigo-600
            "tv" -> Color(0xFF0284C7) // sky-600
            "toys" -> Color(0xFFDB2777) // pink-600
            "mouse" -> Color(0xFF4B5563) // gray-600
            "keyboard" -> Color(0xFF4B5563) // gray-600
            "flashlight_on" -> Color(0xFFCA8A04) // yellow-600
            "schedule" -> Color(0xFF7C3AED) // violet-600
            "location_on" -> Color(0xFF137FEC) // blue-600
            "account_balance_wallet" -> Color(0xFF059669) // emerald-600
            "straighten" -> Color(0xFF4B5563) // gray-600
            "water_drop" -> Color(0xFF0284C7) // sky-600
            else -> Color(0xFF475569)
        }
}
