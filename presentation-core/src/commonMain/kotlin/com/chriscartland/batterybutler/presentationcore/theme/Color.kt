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

// Battery Age Warning Colors (180-364 days)
val BatteryWarningLight = Color(0xFF956D00) // Dark amber (WCAG AA on linen)
val BatteryWarningDark = Color(0xFFE5A100) // Bright amber (WCAG AA on dark)
