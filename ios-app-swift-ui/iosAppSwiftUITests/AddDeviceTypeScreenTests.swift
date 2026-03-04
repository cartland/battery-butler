import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: AddDeviceTypeScreen does not have a separate ContentView.
// The UI body is in AddDeviceTypeScreen and reads directly from
// viewModelWrapper.state (AddDeviceTypeViewModelWrapper).
// The state fields (name, batteryType) are driven through wrapper methods
// (updateName, updateBatteryType) rather than @Binding parameters.
// To enable snapshot tests, extract a stateless AddDeviceTypeContentView:
//
//   struct AddDeviceTypeContentView: View {
//       @Binding var name: String
//       @Binding var batteryType: String
//       let isSaving: Bool
//       let saveError: String?
//       let onSave: () -> Void
//       let onCancel: () -> Void
//   }

final class AddDeviceTypeScreenTests: XCTestCase {
    // No testable ContentView available yet.
    // See comment above for the recommended extraction.
}
