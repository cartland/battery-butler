import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

// SKIPPED: HistoryListScreen does not have a separate ContentView.
// The UI body is in HistoryListScreen and reads directly from
// wrapper.state (HistoryListViewModelWrapper).
// To enable snapshot tests, extract a stateless HistoryListContentView:
//
//   struct HistoryListContentView: View {
//       let state: HistoryListUiState
//       let onEventTap: (String) -> Void
//   }

final class HistoryListScreenTests: XCTestCase {
    // No testable ContentView available yet.
    // See comment above for the recommended extraction.
}
