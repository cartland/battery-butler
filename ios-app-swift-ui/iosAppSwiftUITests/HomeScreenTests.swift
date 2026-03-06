import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class HomeScreenTests: XCTestCase {
    func testHomeContentView_WithDevices() {
        let groupedDevices: [String: [Device]] = [
            "Living Room": [TestData.device],
            "Kitchen": [TestData.device2]
        ]
        let state = HomeUiState(
            groups: [:],
            devices: [:],
            groupedDevices: groupedDevices,
            deviceTypes: [:],
            isSortAscending: true,
            isGroupAscending: true,
            sortOption: .name,
            groupOption: .none,
            exportData: nil,
            syncStatus: SyncStatusIdle()
        )

        let view = HomeContentView(
            state: state,
            onAddDeviceTapped: {},
            onAddEventTapped: {},
            deviceDestination: { _ in Text("Detail") },
            settingsDestination: { Text("Settings") },
            aiDestination: { Text("AI Chat") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testHomeContentView_Empty() {
        let state = HomeUiState(
            groups: [:],
            devices: [:],
            groupedDevices: [:],
            deviceTypes: [:],
            isSortAscending: true,
            isGroupAscending: true,
            sortOption: .name,
            groupOption: .none,
            exportData: nil,
            syncStatus: SyncStatusIdle()
        )

        let view = HomeContentView(
            state: state,
            onAddDeviceTapped: {},
            onAddEventTapped: {},
            deviceDestination: { _ in Text("Detail") },
            settingsDestination: { Text("Settings") },
            aiDestination: { Text("AI Chat") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
