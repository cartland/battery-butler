import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: AddBatteryEventScreen does not have a separate ContentView.
// The UI body is in AddBatteryEventScreen and reads directly from
// wrapper (AddBatteryEventViewModelWrapper) for devices, aiMessages, etc.
// To enable snapshot tests, extract a stateless AddBatteryEventContentView:
//
//   struct AddBatteryEventContentView: View {
//       let devices: [Device]
//       @Binding var selectedDeviceId: String?
//       @Binding var eventDate: Date
//       @Binding var batteryType: String
//       @Binding var notes: String
//       let onSave: () -> Void
//   }

final class AddBatteryEventScreenTests: XCTestCase {
    // No testable ContentView available yet.
    // See comment above for the recommended extraction.
}
