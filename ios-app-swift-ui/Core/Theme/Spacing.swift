import SwiftUI

/// Standard spacing values for consistent layout throughout the app.
/// Mirrors the Android Padding constants in presentation-core/theme/Padding.kt.
/// Use these instead of hardcoded numeric values.
enum Spacing {
    /// 4 pt
    static let extraSmall: CGFloat = 4
    /// 8 pt
    static let small: CGFloat = 8
    /// 12 pt
    static let medium: CGFloat = 12
    /// 16 pt
    static let standard: CGFloat = 16
    /// 24 pt
    static let large: CGFloat = 24
    /// 32 pt
    static let extraLarge: CGFloat = 32
}

/// Standard corner radius values for rounded shapes.
/// Mirrors the Android CornerRadius constants in presentation-core/theme/Padding.kt.
enum CornerRadius {
    /// 4 pt
    static let extraSmall: CGFloat = 4
    /// 8 pt
    static let small: CGFloat = 8
    /// 12 pt
    static let medium: CGFloat = 12
    /// 16 pt
    static let large: CGFloat = 16
    /// 28 pt
    static let extraLarge: CGFloat = 28
}
