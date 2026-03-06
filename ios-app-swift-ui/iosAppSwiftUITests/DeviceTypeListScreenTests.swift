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
        let successState = DeviceTypeListUiStateSuccess(
            groupedTypes: groupedTypes,
            sortOption: .name,
            groupOption: .none,
            isSortAscending: true,
            isGroupAscending: true
        )

        let view = DeviceTypeListContentView(
            state: successState,
            onAddTypeTapped: {},
            editDestination: { _ in Text("Edit") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testDeviceTypeListContentView_Empty() {
        let successState = DeviceTypeListUiStateSuccess(
            groupedTypes: [:],
            sortOption: .name,
            groupOption: .none,
            isSortAscending: true,
            isGroupAscending: true
        )

        let view = DeviceTypeListContentView(
            state: successState,
            onAddTypeTapped: {},
            editDestination: { _ in Text("Edit") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }

    func testDeviceTypeListContentView_Loading() {
        let view = DeviceTypeListContentView(
            state: DeviceTypeListUiStateLoading(),
            onAddTypeTapped: {},
            editDestination: { _ in Text("Edit") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)))
    }
}
