package com.chriscartland.batterybutler.presentationcore.theme

import androidx.compose.ui.graphics.Color

// Comforting Theme: Sage & Linen

val PrimaryLight = Color(0xFF537A66) // Deep Sage (for light theme)
val PrimaryDark = Color(0xFF7CA38F) // Lighter Sage (for dark theme, better contrast)
val Secondary = Color(0xFF8B7355) // Warm Walnut
val Tertiary = Color(0xFF5E7A91) // Soft Steel Blue - for AI/accent elements

// Light Theme
val BackgroundLight = Color(0xFFF7F5EF) // Warm Linen (Not White)
val SurfaceLight = Color(0xFFFEFCF8) // Off-white for cards
val OnBackgroundLight = Color(0xFF2D2926) // Soft Black
val OnSurfaceLight = Color(0xFF2D2926)

// Dark Theme
val BackgroundDark = Color(0xFF191C1A) // Warm Dark Green-Grey
val SurfaceDark = Color(0xFF252927)
val OnBackgroundDark = Color(0xFFE3E2E6)
val OnSurfaceDark = Color(0xFFE3E2E6)

val OnPrimary = Color.White
val OnSecondary = Color.White
val OnTertiary = Color.White

// Error Colors
val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color.White
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val Error = ErrorLight // Backward compatibility if needed, but prefer specific
val OnError = OnErrorLight

// Container colors
val TertiaryContainerLight = Color(0xFFD5E3EC) // Light blue-grey container
val OnTertiaryContainerLight = Color(0xFF19333F) // Dark text on container
val TertiaryContainerDark = Color(0xFF3A5163) // Darker steel blue container
val OnTertiaryContainerDark = Color(0xFFD5E3EC) // Light text on container

// Outline colors
val OutlineLight = Color(0xFF79756C) // Warm grey outline
val OutlineDark = Color(0xFF928F86) // Lighter warm grey for dark mode

// Icon Accent Colors (per device-type category)
// Light theme: tinted backgrounds with saturated foreground icons
// Dark theme: muted tinted backgrounds with lighter foreground icons

data class IconAccent(
    val containerLight: Color,
    val contentLight: Color,
    val containerDark: Color,
    val contentDark: Color,
)

val IconAccentBlue = IconAccent(
    containerLight = Color(0xFFDCE8F5), // Soft blue
    contentLight = Color(0xFF3B6FA0), // Deep blue
    containerDark = Color(0xFF1E3044), // Dark blue
    contentDark = Color(0xFF8CB8E0), // Light blue
)

val IconAccentOrange = IconAccent(
    containerLight = Color(0xFFFAE6D5), // Soft orange
    contentLight = Color(0xFFB86E2D), // Deep orange
    containerDark = Color(0xFF3A2518), // Dark orange
    contentDark = Color(0xFFE8A873), // Light orange
)

val IconAccentPurple = IconAccent(
    containerLight = Color(0xFFECE0F5), // Soft purple
    contentLight = Color(0xFF7B4FA0), // Deep purple
    containerDark = Color(0xFF2C1E3D), // Dark purple
    contentDark = Color(0xFFBB96D8), // Light purple
)

val IconAccentRed = IconAccent(
    containerLight = Color(0xFFF5DDDD), // Soft red
    contentLight = Color(0xFFA04040), // Deep red
    containerDark = Color(0xFF3A1E1E), // Dark red
    contentDark = Color(0xFFD89090), // Light red
)

val IconAccentTeal = IconAccent(
    containerLight = Color(0xFFD5EDE8), // Soft teal
    contentLight = Color(0xFF2D7A6B), // Deep teal
    containerDark = Color(0xFF183530), // Dark teal
    contentDark = Color(0xFF7BC0B0), // Light teal
)

val IconAccentGreen = IconAccent(
    containerLight = Color(0xFFDAEDD8), // Soft green
    contentLight = Color(0xFF3F7A3A), // Deep green
    containerDark = Color(0xFF1E3520), // Dark green
    contentDark = Color(0xFF8FC08A), // Light green
)

val IconAccentPink = IconAccent(
    containerLight = Color(0xFFF5DDE8), // Soft pink
    contentLight = Color(0xFFA04070), // Deep pink
    containerDark = Color(0xFF3A1E2C), // Dark pink
    contentDark = Color(0xFFD890B0), // Light pink
)

val IconAccentSlate = IconAccent(
    containerLight = Color(0xFFE5E5E0), // Soft warm gray
    contentLight = Color(0xFF5A5850), // Deep warm gray
    containerDark = Color(0xFF2A2A28), // Dark warm gray
    contentDark = Color(0xFFA0A098), // Light warm gray
)
