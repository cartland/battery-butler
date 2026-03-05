import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: EditDeviceTypeScreen does not have a separate ContentView.
// The UI body is in EditDeviceTypeScreen and reads directly from
// viewModelWrapper.state (EditDeviceTypeViewModelWrapper).
// The state fields (name, batteryType, isLoading, isNotFound) are managed
// internally by the wrapper, not through @Binding parameters.
// To enable snapshot tests, extract a stateless EditDeviceTypeContentView:
//
//   struct EditDeviceTypeContentView: View {
//       let state: EditDeviceTypeState
//       @Binding var name: String
//       @Binding var batteryType: String
//       let onSave: () -> Void
//       let onDelete: () -> Void
//   }

final class EditDeviceTypeScreenTests: XCTestCase {
    // No testable ContentView available yet.
    // See comment above for the recommended extraction.
}
