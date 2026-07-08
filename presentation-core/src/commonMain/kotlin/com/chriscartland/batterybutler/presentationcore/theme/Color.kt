package com.chriscartland.batterybutler.presentationcore.theme

import androidx.compose.ui.graphics.Color

// Comforting Theme: Sage & Linen
//
// Every Material 3 color role is defined explicitly for both light and dark below.
// Do NOT leave roles unset — an unset role falls back to Material's default (cold,
// purple-tinted) baseline, which clashes with this warm sage/linen identity. The
// neutrals here are deliberately tinted warm green-grey so surfaces, containers,
// dividers, and nav chrome all read as one cohesive palette.

// ---------------------------------------------------------------------------
// Primary — Sage Green
// ---------------------------------------------------------------------------
val PrimaryLight = Color(0xFF3F6B54) // Deep Sage (darkened slightly for AA on linen)
val PrimaryDark = Color(0xFFC4E8D5) // Luminous Sage (tone ~84, brightened for dark-mode contrast/pop)
val OnPrimaryLight = Color.White
val OnPrimaryDark = Color(0xFF003824) // Deep forest
val PrimaryContainerLight = Color(0xFFC1E9D2) // Pastel sage green
val OnPrimaryContainerLight = Color(0xFF00210F) // Deep forest for contrast
val PrimaryContainerDark = Color(0xFF265240) // Dark sage
val OnPrimaryContainerDark = Color(0xFFC1E9D2) // Pastel sage for contrast

// ---------------------------------------------------------------------------
// Secondary — Warm Walnut (sensors/controls/gaming icons)
// ---------------------------------------------------------------------------
val SecondaryLight = Color(0xFF7A6248) // Warm Walnut (darkened for AA on linen)
val SecondaryDark = Color(0xFFEBDAC6) // Light warm tan (tone ~85, brightened for dark-mode contrast/pop)
val OnSecondaryLight = Color.White
val OnSecondaryDark = Color(0xFF412E17) // Deep espresso
val SecondaryContainerLight = Color(0xFFEBDFC8) // Warm cream/linen
val OnSecondaryContainerLight = Color(0xFF2B1E0D) // Deep espresso for contrast
val SecondaryContainerDark = Color(0xFF5A4530) // Dark walnut
val OnSecondaryContainerDark = Color(0xFFEBDFC8) // Warm cream for contrast

// ---------------------------------------------------------------------------
// Tertiary — Steel Blue (AI/accent, electronics/smart home icons)
// ---------------------------------------------------------------------------
val TertiaryLight = Color(0xFF4A6678) // Soft Steel Blue (darkened for AA on linen)
val TertiaryDark = Color(0xFFCEE0EE) // Light steel blue (tone ~87, brightened for dark-mode contrast/pop)
val OnTertiaryLight = Color.White
val OnTertiaryDark = Color(0xFF12333F) // Deep steel
val TertiaryContainerLight = Color(0xFFD5E3EC) // Light blue-grey container
val OnTertiaryContainerLight = Color(0xFF0B2531) // Dark text on container
val TertiaryContainerDark = Color(0xFF35505E) // Darker steel blue container
val OnTertiaryContainerDark = Color(0xFFD5E3EC) // Light text on container

// ---------------------------------------------------------------------------
// Neutral surfaces — Warm Linen (light) / Warm Dark Green-Grey (dark)
// ---------------------------------------------------------------------------
// Light
val BackgroundLight = Color(0xFFF7F5EF) // Warm Linen (Not White)
val OnBackgroundLight = Color(0xFF1B1C18) // Soft Black
val SurfaceLight = Color(0xFFF7F5EF) // Matches background (M3 tonal surface)
val OnSurfaceLight = Color(0xFF1B1C18)
val SurfaceVariantLight = Color(0xFFE1E4D5) // Warm greige — cards, badges, chips
val OnSurfaceVariantLight = Color(0xFF45483F) // Warm grey — secondary text (AA)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF2F0E8)
val SurfaceContainerLight = Color(0xFFECEBE2)
val SurfaceContainerHighLight = Color(0xFFE7E5DC)
val SurfaceContainerHighestLight = Color(0xFFE1DFD7)
val SurfaceDimLight = Color(0xFFDDDCD1)
val SurfaceBrightLight = Color(0xFFF7F5EF)
val InverseSurfaceLight = Color(0xFF30312C)
val InverseOnSurfaceLight = Color(0xFFF1F1E9)
val InversePrimaryLight = Color(0xFF9BD4B5)

// Dark
val BackgroundDark = Color(0xFF191C1A) // Warm Dark Green-Grey
val OnBackgroundDark = Color(0xFFE1E3DD)
val SurfaceDark = Color(0xFF191C1A) // Matches background (M3 tonal surface)
val OnSurfaceDark = Color(0xFFE1E3DD)
val SurfaceVariantDark = Color(0xFF3C4A40) // Warm green-grey — cards, badges, chips
val OnSurfaceVariantDark = Color(0xFFCCD4C9) // Warm light grey — secondary text, brightened (AA)
val SurfaceContainerLowestDark = Color(0xFF0E120F)
val SurfaceContainerLowDark = Color(0xFF1B1F1C)
val SurfaceContainerDark = Color(0xFF1F2320)
val SurfaceContainerHighDark = Color(0xFF2A2E2A)
val SurfaceContainerHighestDark = Color(0xFF353935)
val SurfaceDimDark = Color(0xFF191C1A)
val SurfaceBrightDark = Color(0xFF3F4340)
val InverseSurfaceDark = Color(0xFFE1E3DD)
val InverseOnSurfaceDark = Color(0xFF2D312D)
val InversePrimaryDark = Color(0xFF3F6B54)

// Outline
val OutlineLight = Color(0xFF75786C) // Warm grey outline
val OutlineVariantLight = Color(0xFFC5C8B9) // Subtle dividers
val OutlineDark = Color(0xFF8B948A) // Lighter warm grey for dark mode
val OutlineVariantDark = Color(0xFF424A42) // Subtle dividers

// Scrim (shared)
val Scrim = Color(0xFF000000)

// ---------------------------------------------------------------------------
// Error
// ---------------------------------------------------------------------------
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

// ---------------------------------------------------------------------------
// Battery Age Warning Colors (180-364 days)
// ---------------------------------------------------------------------------
val BatteryWarningLight = Color(0xFF956D00) // Dark amber (WCAG AA on linen)
val BatteryWarningDark = Color(0xFFE5A100) // Bright amber (WCAG AA on dark)
