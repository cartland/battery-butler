import XCTest
import SwiftUI
import SnapshotTesting
import shared
@testable import BatteryButler

final class AddDeviceScreenTests: XCTestCase {
    func testAddDeviceContentView_Empty() {
        let dummyTypes = [
            DeviceType(id: "t1", name: "Smoke Alarm", defaultIcon: "detector_smoke", batteryType: "9V", batteryQuantity: 1),
            DeviceType(id: "t2", name: "TV Remote", defaultIcon: "tv", batteryType: "AAA", batteryQuantity: 2)
        ]

        let view = AddDeviceContentView(
            name: .constant(""),
            location: .constant(""),
            selectedTypeId: .constant(""),
            deviceTypes: dummyTypes,
            onAdd: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }

    func testAddDeviceContentView_Filled() {
        let dummyTypes = [
            DeviceType(id: "t1", name: "Smoke Alarm", defaultIcon: "detector_smoke", batteryType: "9V", batteryQuantity: 1),
            DeviceType(id: "t2", name: "TV Remote", defaultIcon: "tv", batteryType: "AAA", batteryQuantity: 2)
        ]

        let view = AddDeviceContentView(
            name: .constant("Living Room Smoke Alarm"),
            location: .constant("Living Room"),
            selectedTypeId: .constant("t1"),
            deviceTypes: dummyTypes,
            onAdd: {},
            onCancel: {}
        )

        assertSnapshot(of: view, as: .image(layout: .device(config: .iPhone13Pro)), named: "light")
        assertSnapshot(
            of: view.preferredColorScheme(.dark),
            as: .image(layout: .device(config: .iPhone13Pro)),
            named: "dark"
        )
    }
}
