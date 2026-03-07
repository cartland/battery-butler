import SwiftUI

extension Color {
    init(light: Color, dark: Color) {
        self.init(UIColor { traitCollection in
            traitCollection.userInterfaceStyle == .dark
                ? UIColor(dark)
                : UIColor(light)
        })
    }

    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255.0,
            green: Double((hex >> 8) & 0xFF) / 255.0,
            blue: Double(hex & 0xFF) / 255.0,
            opacity: alpha
        )
    }

    // MARK: - Primary

    static let butlerPrimary = Color(light: Color(hex: 0x537A66), dark: Color(hex: 0x7CA38F))
    static let butlerOnPrimary = Color(light: .white, dark: .white)
    static let butlerPrimaryContainer = Color(light: Color(hex: 0xCCE8D7), dark: Color(hex: 0x3A5C4A))
    static let butlerOnPrimaryContainer = Color(light: Color(hex: 0x0D3322), dark: Color(hex: 0xCCE8D7))

    // MARK: - Secondary

    static let butlerSecondary = Color(light: Color(hex: 0x8B7355), dark: Color(hex: 0x8B7355))
    static let butlerOnSecondary = Color(light: .white, dark: .white)
    static let butlerSecondaryContainer = Color(light: Color(hex: 0xEBDFC8), dark: Color(hex: 0x5B4933))
    static let butlerOnSecondaryContainer = Color(light: Color(hex: 0x2B1E0D), dark: Color(hex: 0xEBDFC8))

    // MARK: - Tertiary

    static let butlerTertiary = Color(light: Color(hex: 0x5E7A91), dark: Color(hex: 0x5E7A91))
    static let butlerOnTertiary = Color(light: .white, dark: .white)
    static let butlerTertiaryContainer = Color(light: Color(hex: 0xD5E3EC), dark: Color(hex: 0x3A5163))
    static let butlerOnTertiaryContainer = Color(light: Color(hex: 0x19333F), dark: Color(hex: 0xD5E3EC))

    // MARK: - Background & Surface

    static let butlerBackground = Color(light: Color(hex: 0xF7F5EF), dark: Color(hex: 0x191C1A))
    static let butlerOnBackground = Color(light: Color(hex: 0x2D2926), dark: Color(hex: 0xE3E2E6))
    static let butlerSurface = Color(light: Color(hex: 0xFEFCF8), dark: Color(hex: 0x252927))
    static let butlerOnSurface = Color(light: Color(hex: 0x2D2926), dark: Color(hex: 0xE3E2E6))
    static let butlerSurfaceVariant = Color(light: Color(hex: 0xE8E5DC), dark: Color(hex: 0x49454E))
    static let butlerOnSurfaceVariant = Color(light: Color(hex: 0x49454E), dark: Color(hex: 0xCAC4D0))

    // MARK: - Error

    static let butlerError = Color(light: Color(hex: 0xBA1A1A), dark: Color(hex: 0xFFB4AB))
    static let butlerOnError = Color(light: .white, dark: Color(hex: 0x690005))
    static let butlerErrorContainer = Color(light: Color(hex: 0xFFDAD6), dark: Color(hex: 0x93000A))
    static let butlerOnErrorContainer = Color(light: Color(hex: 0x410002), dark: Color(hex: 0xFFDAD6))

    // MARK: - Outline

    static let butlerOutline = Color(light: Color(hex: 0x79756C), dark: Color(hex: 0x928F86))

    // MARK: - Battery Age Warning

    static let butlerBatteryWarning = Color(light: Color(hex: 0x956D00), dark: Color(hex: 0xE5A100))
}
