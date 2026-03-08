import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class DeviceDetailScreenTests: XCTestCase {
    func testDeviceDetailContentView_Success() {
        let dummyDevice = Device(
            id: "d1",
            name: "Living Room Remote",
            typeId: "t1",
            batteryLastReplaced: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000), // Jan 1, 2024
            lastUpdated: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
            location: "Living Room",
            imagePath: nil
        )

        let dummyType = DeviceType(
            id: "t1",
            name: "TV Remote",
            defaultIcon: "tv",
            batteryType: "AAA",
            batteryQuantity: 2
        )

        let successState = DeviceDetailUiStateSuccess(
            device: dummyDevice,
            deviceType: dummyType,
            events: []
        )

        let view = DeviceDetailContentView(
            state: successState,
            onRecordReplacement: {},
            eventDestination: { _ in Text("Event Details") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testDeviceDetailContentView_Loading() {
        let view = DeviceDetailContentView(
            state: DeviceDetailUiStateLoading(),
            onRecordReplacement: {},
            eventDestination: { _ in Text("Event Details") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testDeviceDetailContentView_NotFound() {
        let view = DeviceDetailContentView(
            state: DeviceDetailUiStateNotFound(),
            onRecordReplacement: {},
            eventDestination: { _ in Text("Event Details") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
