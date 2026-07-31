import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class DeviceTypeListScreenTests: XCTestCase {
    func testDeviceTypeListContentView_Success() {
        let groupedTypes: [String: [DeviceType]] = [
            "Electronics": [TestData.deviceType],
            "Safety": [TestData.deviceType2]
        ]
        let successState = DeviceTypeListScreenStateSuccess(
            groupedTypes: groupedTypes,
            sortOption: .name,
            groupOption: .none,
            isSortAscending: true,
            isGroupAscending: true,
            densityOption: .expanded
        )

        let view = DeviceTypeListContentView(
            state: successState,
            onAddTypeTapped: {},
            onSortOptionSelected: { _ in },
            onGroupOptionSelected: { _ in },
            onSortDirectionToggle: {},
            onGroupDirectionToggle: {},
            detailDestination: { _ in Text("Edit") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testDeviceTypeListContentView_Empty() {
        let successState = DeviceTypeListScreenStateSuccess(
            groupedTypes: [:],
            sortOption: .name,
            groupOption: .none,
            isSortAscending: true,
            isGroupAscending: true,
            densityOption: .expanded
        )

        let view = DeviceTypeListContentView(
            state: successState,
            onAddTypeTapped: {},
            onSortOptionSelected: { _ in },
            onGroupOptionSelected: { _ in },
            onSortDirectionToggle: {},
            onGroupDirectionToggle: {},
            detailDestination: { _ in Text("Edit") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testDeviceTypeListContentView_Loading() {
        let view = DeviceTypeListContentView(
            state: DeviceTypeListScreenStateLoading(),
            onAddTypeTapped: {},
            onSortOptionSelected: { _ in },
            onGroupOptionSelected: { _ in },
            onSortDirectionToggle: {},
            onGroupDirectionToggle: {},
            detailDestination: { _ in Text("Edit") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
