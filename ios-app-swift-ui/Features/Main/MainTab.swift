import Foundation
import SwiftUI

enum MainTab: String, CaseIterable {
    case devices = "tab.devices"
    case types = "tab.types"
    case history = "tab.history"

    var iconName: String {
        switch self {
        case .devices: return "house.fill"
        case .types: return "list.bullet"
        case .history: return "clock.fill"
        }
    }

    /// Localized tab name for use in non-LocalizedStringKey contexts.
    var localizedName: String {
        String(localized: String.LocalizationValue(rawValue))
    }

    /// Localization key for use in SwiftUI views that accept LocalizedStringKey.
    var titleKey: LocalizedStringKey {
        LocalizedStringKey(rawValue)
    }
}
