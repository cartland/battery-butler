import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class DeviceTypeDetailScreenTests: XCTestCase {
    func testDeviceTypeDetailContentView_Success() {
        let dummyType = DeviceType(
            id: "t1",
            name: "TV Remote",
            defaultIcon: "tv",
            batteryType: "AAA",
            batteryQuantity: 2
        )

        let dummyDevices = [
            Device(
                id: "d1",
                name: "Living Room Remote",
                typeId: "t1",
                batteryLastReplaced: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
                lastUpdated: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
                location: "Living Room",
                imagePath: nil
            ),
            Device(
                id: "d2",
                name: "Bedroom Remote",
                typeId: "t1",
                batteryLastReplaced: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
                lastUpdated: KotlinInstant.Companion.shared.fromEpochMilliseconds(epochMilliseconds: 1704067200000),
                location: "Bedroom",
                imagePath: nil
            ),
        ]

        let successState = DeviceTypeDetailUiStateSuccess(
            deviceType: dummyType,
            devices: dummyDevices
        )

        let view = DeviceTypeDetailContentView(
            state: successState,
            deviceDestination: { _ in Text("Device Details") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testDeviceTypeDetailContentView_Loading() {
        let view = DeviceTypeDetailContentView(
            state: DeviceTypeDetailUiStateLoading(),
            deviceDestination: { _ in Text("Device Details") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testDeviceTypeDetailContentView_NotFound() {
        let view = DeviceTypeDetailContentView(
            state: DeviceTypeDetailUiStateNotFound(),
            deviceDestination: { _ in Text("Device Details") }
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
