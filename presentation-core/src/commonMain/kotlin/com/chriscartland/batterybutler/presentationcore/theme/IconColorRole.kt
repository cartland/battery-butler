package com.chriscartland.batterybutler.presentationcore.theme

/**
 * Semantic color role for device icons.
 *
 * Maps icon categories to Material Design 3 color roles so icon accent colors
 * always derive from the app's design token system and automatically adapt
 * to light/dark theme and any future theme changes.
 */
enum class IconColorRole {
    /** Sage green — nature, climate, time, home essentials. Maps to primaryContainer. */
    Primary,

    /** Warm walnut — tools, sensors, controls, gaming. Maps to secondaryContainer. */
    Secondary,

    /** Steel blue — electronics, smart home, tech. Maps to tertiaryContainer. */
    Tertiary,

    /** Red — safety-critical devices (smoke/CO detectors). Maps to errorContainer. */
    Error,

    /** Neutral warm gray — catch-all default. Maps to surfaceVariant. */
    Surface,
}
