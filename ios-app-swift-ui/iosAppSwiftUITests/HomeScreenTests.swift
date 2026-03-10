import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class HomeScreenTests: XCTestCase {
    private func makeView(state: HomeUiState) -> some View {
        HomeContentView(
            state: state,
            onAddDeviceTapped: {},
            onAddEventTapped: {},
            onSortOptionSelected: { _ in },
            onGroupOptionSelected: { _ in },
            onSortDirectionToggle: {},
            onGroupDirectionToggle: {},
            deviceDestination: { _ in Text("Detail") },
            settingsDestination: { Text("Settings") },
            aiDestination: { Text("AI Chat") }
        )
    }

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
            syncStatus: SyncStatusIdle(),
            error: nil
        )

        let view = makeView(state: state)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
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
            syncStatus: SyncStatusIdle(),
            error: nil
        )

        let view = makeView(state: state)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testHomeContentView_Syncing() {
        let groupedDevices: [String: [Device]] = [
            "Living Room": [TestData.device]
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
            syncStatus: SyncStatusSyncing(),
            error: nil
        )

        let view = makeView(state: state)

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
