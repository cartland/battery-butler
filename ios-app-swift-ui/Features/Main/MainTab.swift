import Foundation
import SwiftUI

enum MainTab: String, CaseIterable {
    case devices = "Devices"
    case types = "Types"
    case history = "History"
    
    var iconName: String {
        switch self {
        case .devices: return "house.fill"
        case .types: return "list.bullet"
        case .history: return "clock.fill"
        }
    }
}
