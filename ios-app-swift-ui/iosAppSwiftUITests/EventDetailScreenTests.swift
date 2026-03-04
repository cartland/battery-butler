import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: EventDetailScreen does not have a separate ContentView.
// The UI body is in EventDetailScreen and reads directly from
// wrapper.state (EventDetailViewModelWrapper) and NativeComponent.
// To enable snapshot tests, extract a stateless EventDetailContentView:
//
//   struct EventDetailContentView: View {
//       let state: EventDetailUiState
//   }

final class EventDetailScreenTests: XCTestCase {
    // No testable ContentView available yet.
    // See comment above for the recommended extraction.
}
