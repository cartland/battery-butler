import SwiftUI

enum BatteryAgeHelper {
    static func batteryAgeColor(days: Int?) -> Color {
        guard let days = days else {
            return .butlerOnSurfaceVariant
        }
        switch days {
        case 365...:
            return .butlerError
        case 180..<365:
            return .butlerBatteryWarning
        default:
            return .butlerOnSurfaceVariant
        }
    }

    static func batteryAgeFontWeight(days: Int?) -> Font.Weight {
        guard let days = days, days >= 365 else { return .regular }
        return .bold
    }

    static func daysSince(epochMilliseconds: Int64) -> Int? {
        let date = Date(timeIntervalSince1970: TimeInterval(epochMilliseconds) / 1000.0)
        let calendar = Calendar.current
        let components = calendar.dateComponents([.day], from: date, to: Date())
        return components.day
    }
}
